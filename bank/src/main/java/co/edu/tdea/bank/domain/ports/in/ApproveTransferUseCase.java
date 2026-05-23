package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Transfer;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Input port — approves a pending transfer and executes the fund movement.
 * Only users with role {@code COMPANY_SUPERVISOR} or {@code BUSINESS_ADMIN} may perform this.
 */
public interface ApproveTransferUseCase {

    /**
     * @param transferId       identifier of the transfer to approve
     * @param approverId       UUID of the user granting the approval
     * @param approvalDateTime moment the approval is granted
     * @return the updated {@link Transfer} in {@code EXECUTED} status
     */
    Transfer approveTransfer(Long transferId,
                             UUID approverId,
                             LocalDateTime approvalDateTime);
}
