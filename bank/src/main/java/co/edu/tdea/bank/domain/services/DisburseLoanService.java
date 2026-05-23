package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.in.DisburseLoanUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.Exceptions.InvalidStateTransitionException;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service â€” orchestrates the disbursement of an approved loan.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The loan must exist.</li>
 *   <li>The analyst user must exist.</li>
 *   <li>The analyst must hold the {@code INTERNAL_ANALYST} role.</li>
 *   <li>The destination account must exist.</li>
 *   <li>State transition {@code APPROVED â†’ DISBURSED} and account-active check
 *       are enforced by the domain; any {@code IllegalStateException} is translated
 *       to {@link InvalidStateTransitionException}.</li>
 *   <li>Both the updated loan and the credited account are persisted.</li>
 *   <li>The operation is audited with balance snapshot for traceability.</li>
 * </ol>
 */
@Service
public class DisburseLoanService implements DisburseLoanUseCase {

    private final LoanRepositoryPort loanRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final BankAccountRepositoryPort bankAccountRepositoryPort;
    private final AuditLogPort auditLogPort;

    public DisburseLoanService(LoanRepositoryPort loanRepositoryPort,
                               UserRepositoryPort userRepositoryPort,
                               BankAccountRepositoryPort bankAccountRepositoryPort,
                               AuditLogPort auditLogPort) {
        this.loanRepositoryPort        = loanRepositoryPort;
        this.userRepositoryPort        = userRepositoryPort;
        this.bankAccountRepositoryPort = bankAccountRepositoryPort;
        this.auditLogPort              = auditLogPort;
    }

    @Override
    public Loan disburseLoan(Long loanId,
                             String destinationAccountNumber,
                             LocalDate disbursementDate,
                             UUID analystUserId) {

        // 1. Loan must exist
        Loan loan = loanRepositoryPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        // 2. Analyst user must exist
        User analyst = userRepositoryPort.findById(analystUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", analystUserId));

        // 3. Only INTERNAL_ANALYST may disburse loans
        if (analyst.getSystemRole() != SystemRole.INTERNAL_ANALYST) {
            throw new UnauthorizedOperationException(
                    "User " + analystUserId + " with role " + analyst.getSystemRole()
                    + " is not authorized to disburse loans. Required role: INTERNAL_ANALYST.");
        }

        // 4. Destination account must exist
        BankAccount destination = bankAccountRepositoryPort
                .findByAccountNumber(destinationAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BankAccount", destinationAccountNumber));

        // 5. Capture balance before disbursement for the audit trail
        BigDecimal balanceBefore = destination.getCurrentBalance();

        // 6. Apply domain state transition â€” validates APPROVED status and account ACTIVE.
        //    Translate any IllegalStateException to InvalidStateTransitionException so
        //    the application layer speaks consistently in domain exceptions.
        try {
            loan.disburse(disbursementDate, destination);
        } catch (IllegalStateException e) {
            throw new InvalidStateTransitionException(
                    "Loan", loan.getLoanStatus(), "disburse");
        }

        // 7. Persist updated loan
        Loan savedLoan = loanRepositoryPort.save(loan);

        // 8. Persist credited account
        bankAccountRepositoryPort.save(destination);

        // 9. Audit â€” include balance snapshot as required by the PDF spec
        auditLogPort.save(
                "Loan",
                String.valueOf(savedLoan.getLoanId()),
                "LOAN_DISBURSED",
                analystUserId.toString(),
                LocalDateTime.now(),
                "destinationAccount=" + destinationAccountNumber
                + ", approvedAmount=" + savedLoan.getApprovedAmount()
                + ", balanceBefore=" + balanceBefore
                + ", balanceAfter=" + destination.getCurrentBalance()
                + ", disbursementDate=" + disbursementDate
                + ", analystRole=INTERNAL_ANALYST"
        );

        return savedLoan;
    }
}
