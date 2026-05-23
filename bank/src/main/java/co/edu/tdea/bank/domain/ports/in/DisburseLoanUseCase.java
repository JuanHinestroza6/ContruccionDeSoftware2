package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Loan;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Input port — disburses an approved loan into the client's target account.
 * The account and analyst are resolved internally from their repositories.
 * Only a user with role {@code INTERNAL_ANALYST} may perform this operation.
 */
public interface DisburseLoanUseCase {

    /**
     * @param loanId                    identifier of the loan to disburse
     * @param destinationAccountNumber  number of the active account that will receive the funds
     * @param disbursementDate          date the funds are transferred
     * @param analystUserId             UUID of the INTERNAL_ANALYST performing the disbursement
     * @return the updated {@link Loan} in {@code DISBURSED} status
     */
    Loan disburseLoan(Long loanId,
                      String destinationAccountNumber,
                      LocalDate disbursementDate,
                      UUID analystUserId);
}
