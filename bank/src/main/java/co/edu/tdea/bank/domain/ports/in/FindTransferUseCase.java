package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Transfer;

import java.util.List;
import java.util.UUID;

/**
 * Input port — query operations for the {@link Transfer} aggregate.
 */
public interface FindTransferUseCase {

    /**
     * Finds a transfer by its technical identifier.
     * Throws {@code ResourceNotFoundException} if no transfer exists with the given ID.
     *
     * @param transferId technical identifier of the transfer
     * @return the transfer
     */
    Transfer findById(Long transferId);

    /**
     * Returns all transfers where the given account is the source.
     * Returns an empty list if no transfers exist for the account (does not throw).
     *
     * @param accountNumber source account number
     * @return list of transfers (possibly empty)
     */
    List<Transfer> findBySourceAccountNumber(String accountNumber);

    /**
     * Returns all transfers initiated by the given user.
     * Returns an empty list if the user has no transfers (does not throw).
     *
     * @param userId technical UUID of the user
     * @return list of transfers (possibly empty)
     */
    List<Transfer> findByCreatedByUserId(UUID userId);
}
