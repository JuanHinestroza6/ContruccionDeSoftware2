package co.edu.tdea.bank.application.usecase;

import co.edu.tdea.bank.application.port.in.ApproveLoanUseCase;
import co.edu.tdea.bank.application.port.out.AuditLogPort;
import co.edu.tdea.bank.application.port.out.LoanRepositoryPort;
import co.edu.tdea.bank.application.port.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.model.Loan;
import co.edu.tdea.bank.domain.model.User;
import co.edu.tdea.bank.shared.exception.InvalidStateTransitionException;
import co.edu.tdea.bank.shared.exception.ResourceNotFoundException;
import co.edu.tdea.bank.shared.exception.UnauthorizedOperationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service — orchestrates the approval of a loan under review.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The loan must exist.</li>
 *   <li>The approving user must exist.</li>
 *   <li>The approving user must hold the {@code INTERNAL_ANALYST} role.</li>
 *   <li>State transition {@code UNDER_REVIEW → APPROVED} is enforced by the domain.</li>
 *   <li>The updated loan is persisted and the operation is audited.</li>
 * </ol>
 */
@Service
public class ApproveLoanService implements ApproveLoanUseCase {

    private final LoanRepositoryPort loanRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogPort auditLogPort;

    public ApproveLoanService(LoanRepositoryPort loanRepositoryPort,
                              UserRepositoryPort userRepositoryPort,
                              AuditLogPort auditLogPort) {
        this.loanRepositoryPort = loanRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.auditLogPort       = auditLogPort;
    }

    @Override
    public Loan approveLoan(Long loanId,
                            UUID approverId,
                            BigDecimal approvedAmount,
                            BigDecimal interestRate,
                            LocalDate approvalDate) {

        // 1. Loan must exist
        Loan loan = loanRepositoryPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        // 2. Approver must exist
        User approver = userRepositoryPort.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        // 3. Approver must be INTERNAL_ANALYST — only this role may approve loans
        if (approver.getSystemRole() != SystemRole.INTERNAL_ANALYST) {
            throw new UnauthorizedOperationException(
                    "User " + approverId + " with role " + approver.getSystemRole()
                    + " is not authorized to approve loans. Required role: INTERNAL_ANALYST.");
        }

        // 4. Apply domain state transition — translate IllegalStateException so the
        //    application layer speaks consistently in domain exceptions
        try {
            loan.approve(approvedAmount, interestRate, approvalDate);
        } catch (IllegalStateException e) {
            throw new InvalidStateTransitionException(
                    "Loan", loan.getLoanStatus(), "approve");
        }

        // 5. Persist
        Loan savedLoan = loanRepositoryPort.save(loan);

        // 6. Audit
        auditLogPort.save(
                "Loan",
                String.valueOf(savedLoan.getLoanId()),
                "LOAN_APPROVED",
                approverId.toString(),
                LocalDateTime.now(),
                "approvedAmount=" + approvedAmount
                + ", interestRate=" + interestRate
                + ", approvalDate=" + approvalDate
                + ", approverRole=INTERNAL_ANALYST"
        );

        return savedLoan;
    }
}
