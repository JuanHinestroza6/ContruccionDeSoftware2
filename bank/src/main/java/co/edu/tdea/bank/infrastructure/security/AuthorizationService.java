package co.edu.tdea.bank.infrastructure.security;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ownership-aware authorization helper used from {@code @PreAuthorize} SpEL.
 *
 * <p>Registered as a bean named {@code "authz"} so controllers can write
 * expressions such as
 * {@code @PreAuthorize("hasRole('INTERNAL_ANALYST') or @authz.hasAccessToLoan(authentication, #loanId)")}.
 *
 * <h2>Authorization model</h2>
 * <ul>
 *   <li><b>Staff bypass roles</b> — {@code INTERNAL_ANALYST},
 *       {@code TELLER_EMPLOYEE} and {@code COMMERCIAL_EMPLOYEE} have read access
 *       to every client, account and loan; ownership checks return {@code true}
 *       unconditionally for these roles.</li>
 *   <li><b>Owner roles</b> — {@code INDIVIDUAL_CLIENT}, {@code BUSINESS_ADMIN},
 *       {@code COMPANY_OPERATOR}, {@code COMPANY_SUPERVISOR} are granted access
 *       only when the resource's owning {@code Client} matches the principal's
 *       {@code relatedClientId}. For company users this means everyone bound to
 *       the same business client sees the same set of resources, which is the
 *       intended behaviour for shared corporate products.</li>
 * </ul>
 *
 * <h2>Information leakage</h2>
 * If the resource lookup fails (id does not exist) this service returns
 * {@code false} rather than letting an exception bubble up. The result is a
 * 403 produced by the {@code @PreAuthorize} layer — never a 404 — so the API
 * never reveals whether the unknown id existed and was forbidden, or simply
 * did not exist. The bypass roles take a different path: their {@code true}
 * result lets the controller proceed, where a genuine 404 may then be returned
 * if the resource truly does not exist.
 */
@Service("authz")
public class AuthorizationService {

    /**
     * Staff roles that bypass ownership filtering entirely. Kept as an enum
     * set to make role membership checks cheap and the intent explicit.
     */
    private static final Set<SystemRole> STAFF_BYPASS_ROLES = Set.of(
            SystemRole.INTERNAL_ANALYST,
            SystemRole.TELLER_EMPLOYEE,
            SystemRole.COMMERCIAL_EMPLOYEE
    );

    private final LoanRepositoryPort loanRepositoryPort;
    private final BankAccountRepositoryPort bankAccountRepositoryPort;

    public AuthorizationService(LoanRepositoryPort loanRepositoryPort,
                                BankAccountRepositoryPort bankAccountRepositoryPort) {
        this.loanRepositoryPort        = loanRepositoryPort;
        this.bankAccountRepositoryPort = bankAccountRepositoryPort;
    }

    /**
     * Verifies the authenticated user can act on the given {@code clientId}.
     *
     * <p>Staff bypass roles always return {@code true}. Owner roles return
     * {@code true} iff their {@code relatedClientId} matches the supplied id.
     * Anything else (null principal, missing client binding) returns
     * {@code false} so the {@code @PreAuthorize} layer produces a 403.
     */
    public boolean hasAccessToClient(Authentication authentication, UUID clientId) {
        if (clientId == null) {
            return false;
        }
        BankUserPrincipal principal = extractPrincipal(authentication);
        if (principal == null) {
            return false;
        }
        if (isStaffBypass(principal)) {
            return true;
        }
        UUID related = principal.getRelatedClientId();
        return related != null && related.equals(clientId);
    }

    /**
     * Verifies the authenticated user can read the given {@code loanId}.
     *
     * <p>For owner roles the loan's {@code applicantClient.clientId} must match
     * the principal's {@code relatedClientId}. If the loan does not exist we
     * return {@code false} (yielding 403) so we never reveal the existence of
     * an unrelated id to a client-tier user.
     */
    public boolean hasAccessToLoan(Authentication authentication, Long loanId) {
        if (loanId == null) {
            return false;
        }
        BankUserPrincipal principal = extractPrincipal(authentication);
        if (principal == null) {
            return false;
        }
        if (isStaffBypass(principal)) {
            return true;
        }
        UUID related = principal.getRelatedClientId();
        if (related == null) {
            return false;
        }
        Optional<Loan> loan = loanRepositoryPort.findById(loanId);
        return loan
                .map(l -> related.equals(l.getApplicantClient().getClientId()))
                .orElse(false);
    }

    /**
     * Verifies the authenticated user can read the given {@code accountNumber}.
     *
     * <p>Same shape as {@link #hasAccessToLoan} — staff bypass, then ownership
     * via the account holder's {@code clientId}. Unknown account numbers
     * resolve to {@code false} on purpose so the API does not leak which
     * account numbers exist.
     */
    public boolean hasAccessToAccount(Authentication authentication, String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return false;
        }
        BankUserPrincipal principal = extractPrincipal(authentication);
        if (principal == null) {
            return false;
        }
        if (isStaffBypass(principal)) {
            return true;
        }
        UUID related = principal.getRelatedClientId();
        if (related == null) {
            return false;
        }
        Optional<BankAccount> account = bankAccountRepositoryPort.findByAccountNumber(accountNumber);
        return account
                .map(a -> related.equals(a.getHolder().getClientId()))
                .orElse(false);
    }

    /**
     * Pulls the {@link BankUserPrincipal} out of the Spring {@link Authentication}.
     * Returns {@code null} when there is no authentication, when the principal
     * is anonymous, or when the principal is not a {@link BankUserPrincipal}
     * (defensive — should never happen once {@code BankUserDetailsService} is in
     * the chain, but we want to fail closed rather than throw a ClassCastException
     * up the filter stack).
     */
    private BankUserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object raw = authentication.getPrincipal();
        if (raw instanceof BankUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private boolean isStaffBypass(BankUserPrincipal principal) {
        SystemRole role;
        try {
            role = SystemRole.valueOf(principal.getRole());
        } catch (IllegalArgumentException ex) {
            // Unknown role string in the principal — fail closed.
            return false;
        }
        return STAFF_BYPASS_ROLES.contains(role);
    }
}
