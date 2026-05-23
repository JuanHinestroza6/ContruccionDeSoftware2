package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Transfer;

import java.util.UUID;

/**
 * Input port — rejects a pending transfer without moving any funds.
 * Only users with role {@code COMPANY_SUPERVISOR} or {@code BUSINESS_ADMIN} may perform this.
 */
public interface RejectTransferUseCase {

    /**
     * @param transferId identifier of the transfer to reject
     * @param approverId UUID of the user rejecting the transfer
     * @return the updated {@link Transfer} in {@code REJECTED} status
     */
    Transfer rejectTransfer(Long transferId, UUID approverId);
}
