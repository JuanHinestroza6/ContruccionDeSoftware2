package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Loan;

import java.util.UUID;

/**
 * Input port — rejects a loan that is currently under review.
 * Only a user with role {@code INTERNAL_ANALYST} may perform this operation.
 */
public interface RejectLoanUseCase {

    /**
     * @param loanId     identifier of the loan to reject
     * @param approverId UUID of the user performing the rejection
     * @return the updated {@link Loan} in {@code REJECTED} status
     */
    Loan rejectLoan(Long loanId, UUID approverId);
}
