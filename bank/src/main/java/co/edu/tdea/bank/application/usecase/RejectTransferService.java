package co.edu.tdea.bank.application.usecase;

import co.edu.tdea.bank.application.port.in.RejectTransferUseCase;
import co.edu.tdea.bank.application.port.out.AuditLogPort;
import co.edu.tdea.bank.application.port.out.TransferRepositoryPort;
import co.edu.tdea.bank.application.port.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.model.Transfer;
import co.edu.tdea.bank.domain.model.User;
import co.edu.tdea.bank.shared.exception.InvalidStateTransitionException;
import co.edu.tdea.bank.shared.exception.ResourceNotFoundException;
import co.edu.tdea.bank.shared.exception.UnauthorizedOperationException;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service — orchestrates the rejection of a pending transfer.
 * No funds are moved; the transfer moves to a terminal {@code REJECTED} state.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The transfer must exist.</li>
 *   <li>The rejecting user must exist.</li>
 *   <li>The rejecting user must hold {@code COMPANY_SUPERVISOR} or {@code BUSINESS_ADMIN}.</li>
 *   <li>Domain enforces {@code PENDING_APPROVAL → REJECTED} transition; any
 *       {@code IllegalStateException} is translated to {@link InvalidStateTransitionException}.</li>
 *   <li>The updated transfer is persisted and the operation is audited.</li>
 * </ol>
 */
@Service
public class RejectTransferService implements RejectTransferUseCase {

    private final TransferRepositoryPort transferRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogPort auditLogPort;

    public RejectTransferService(TransferRepositoryPort transferRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 AuditLogPort auditLogPort) {
        this.transferRepositoryPort = transferRepositoryPort;
        this.userRepositoryPort     = userRepositoryPort;
        this.auditLogPort           = auditLogPort;
    }

    @Override
    public Transfer rejectTransfer(Long transferId, UUID approverId) {

        // 1. Transfer must exist
        Transfer transfer = transferRepositoryPort.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", transferId));

        // 2. Rejecting user must exist
        User approver = userRepositoryPort.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        // 3. Only COMPANY_SUPERVISOR or BUSINESS_ADMIN may reject transfers
        SystemRole role = approver.getSystemRole();
        if (role != SystemRole.COMPANY_SUPERVISOR && role != SystemRole.BUSINESS_ADMIN) {
            throw new UnauthorizedOperationException(
                    "User " + approverId + " with role " + role
                    + " is not authorized to reject transfers. "
                    + "Required role: COMPANY_SUPERVISOR or BUSINESS_ADMIN.");
        }

        // 4. Apply domain state transition — translate IllegalStateException so the
        //    application layer speaks consistently in domain exceptions
        try {
            transfer.reject(approver);
        } catch (IllegalStateException e) {
            throw new InvalidStateTransitionException(
                    "Transfer", transfer.getTransferStatus(), "reject");
        }

        // 5. Persist
        Transfer savedTransfer = transferRepositoryPort.save(transfer);

        // 6. Audit
        auditLogPort.save(
                "Transfer",
                String.valueOf(savedTransfer.getTransferId()),
                "TRANSFER_REJECTED",
                approverId.toString(),
                LocalDateTime.now(),
                "rejectorRole=" + role
                + ", sourceAccount=" + savedTransfer.getSourceAccount().getAccountNumber()
                + ", destinationAccount=" + savedTransfer.getDestinationAccount().getAccountNumber()
                + ", amount=" + savedTransfer.getAmount()
        );

        return savedTransfer;
    }
}
