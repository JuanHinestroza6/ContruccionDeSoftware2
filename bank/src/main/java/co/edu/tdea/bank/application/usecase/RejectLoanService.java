package co.edu.tdea.bank.application.usecase;

import co.edu.tdea.bank.application.port.in.RejectLoanUseCase;
import co.edu.tdea.bank.application.port.out.AuditLogPort;
import co.edu.tdea.bank.application.port.out.LoanRepositoryPort;
import co.edu.tdea.bank.application.port.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.model.Loan;
import co.edu.tdea.bank.domain.model.User;
import co.edu.tdea.bank.shared.exception.InvalidStateTransitionException;
import co.edu.tdea.bank.shared.exception.ResourceNotFoundException;
import co.edu.tdea.bank.shared.exception.UnauthorizedOperationException;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service — orchestrates the rejection of a loan under review.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The loan must exist.</li>
 *   <li>The rejecting user must exist.</li>
 *   <li>The rejecting user must hold the {@code INTERNAL_ANALYST} role.</li>
 *   <li>State transition {@code UNDER_REVIEW → REJECTED} is enforced by the domain.</li>
 *   <li>The updated loan is persisted and the operation is audited.</li>
 * </ol>
 */
@Service
public class RejectLoanService implements RejectLoanUseCase {

    private final LoanRepositoryPort loanRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogPort auditLogPort;

    public RejectLoanService(LoanRepositoryPort loanRepositoryPort,
                             UserRepositoryPort userRepositoryPort,
                             AuditLogPort auditLogPort) {
        this.loanRepositoryPort = loanRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.auditLogPort       = auditLogPort;
    }

    @Override
    public Loan rejectLoan(Long loanId, UUID approverId) {

        // 1. Loan must exist
        Loan loan = loanRepositoryPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        // 2. Rejecting user must exist
        User approver = userRepositoryPort.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        // 3. Only INTERNAL_ANALYST may reject loans
        if (approver.getSystemRole() != SystemRole.INTERNAL_ANALYST) {
            throw new UnauthorizedOperationException(
                    "User " + approverId + " with role " + approver.getSystemRole()
                    + " is not authorized to reject loans. Required role: INTERNAL_ANALYST.");
        }

        // 4. Apply domain state transition — translate IllegalStateException so the
        //    application layer speaks consistently in domain exceptions
        try {
            loan.reject();
        } catch (IllegalStateException e) {
            throw new InvalidStateTransitionException(
                    "Loan", loan.getLoanStatus(), "reject");
        }

        // 5. Persist
        Loan savedLoan = loanRepositoryPort.save(loan);

        // 6. Audit
        auditLogPort.save(
                "Loan",
                String.valueOf(savedLoan.getLoanId()),
                "LOAN_REJECTED",
                approverId.toString(),
                LocalDateTime.now(),
                "rejectorRole=INTERNAL_ANALYST"
        );

        return savedLoan;
    }
}
