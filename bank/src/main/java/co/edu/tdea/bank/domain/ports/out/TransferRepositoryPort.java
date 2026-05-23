package co.edu.tdea.bank.domain.ports.out;

import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.models.Transfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — persistence contract for {@link Transfer} aggregate.
 */
public interface TransferRepositoryPort {

    /** Persists a new transfer or updates an existing one. Returns the saved instance. */
    Transfer save(Transfer transfer);

    /** Finds a transfer by its technical identity. */
    Optional<Transfer> findById(Long transferId);

    /** Returns all transfers where the given account is the source. */
    List<Transfer> findBySourceAccountNumber(String accountNumber);

    /** Returns all transfers where the given account is the destination. */
    List<Transfer> findByDestinationAccountNumber(String accountNumber);

    /** Returns all transfers currently in the given status. */
    List<Transfer> findByStatus(TransferStatus status);

    /**
     * Returns all transfers initiated by the given user, in any status.
     * Used to build the user's operation history.
     */
    List<Transfer> findByCreatedByUserId(UUID userId);
}
