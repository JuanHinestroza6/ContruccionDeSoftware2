package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Loan;

import java.util.List;
import java.util.UUID;

/**
 * Input port — query operations for the {@link Loan} aggregate.
 */
public interface FindLoanUseCase {

    /**
     * Finds a loan by its technical identifier.
     * Throws {@code ResourceNotFoundException} if no loan exists with the given ID.
     *
     * @param loanId technical identifier of the loan
     * @return the loan
     */
    Loan findById(Long loanId);

    /**
     * Returns all loans belonging to the given client.
     * Returns an empty list if the client has no loans (does not throw).
     *
     * @param clientId technical UUID of the client
     * @return list of loans (possibly empty)
     */
    List<Loan> findByClientId(UUID clientId);
}
