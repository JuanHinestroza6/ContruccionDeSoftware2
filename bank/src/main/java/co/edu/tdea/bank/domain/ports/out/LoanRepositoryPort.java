package co.edu.tdea.bank.domain.ports.out;

import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.models.Loan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — persistence contract for {@link Loan} aggregate.
 */
public interface LoanRepositoryPort {

    /** Persists a new loan or updates an existing one. Returns the saved instance. */
    Loan save(Loan loan);

    /** Finds a loan by its technical identity. */
    Optional<Loan> findById(Long loanId);

    /** Returns all loans associated with the given client. */
    List<Loan> findByClientId(UUID clientId);

    /** Returns all loans currently in the given status. */
    List<Loan> findByStatus(LoanStatus status);

    /** Returns all loans for the given client that are in the given status. */
    List<Loan> findByClientIdAndStatus(UUID clientId, LoanStatus status);
}
