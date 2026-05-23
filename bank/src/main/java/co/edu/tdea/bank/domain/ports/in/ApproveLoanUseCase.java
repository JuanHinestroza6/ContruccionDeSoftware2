package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Input port — approves a loan that is currently under review.
 * Only a user with role {@code INTERNAL_ANALYST} may perform this operation.
 */
public interface ApproveLoanUseCase {

    /**
     * @param loanId         identifier of the loan to approve
     * @param approverId     UUID of the user performing the approval
     * @param approvedAmount amount the bank agrees to lend; must be > 0
     * @param interestRate   annual interest rate applied; must be >= 0
     * @param approvalDate   date of the approval decision
     * @return the updated {@link Loan} in {@code APPROVED} status
     */
    Loan approveLoan(Long loanId,
                     UUID approverId,
                     BigDecimal approvedAmount,
                     BigDecimal interestRate,
                     LocalDate approvalDate);
}
