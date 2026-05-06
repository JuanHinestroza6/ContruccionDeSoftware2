package co.edu.tdea.bank.application.usecase;

import co.edu.tdea.bank.application.port.in.ExpirePendingTransfersUseCase;
import co.edu.tdea.bank.application.port.out.AuditLogPort;
import co.edu.tdea.bank.application.port.out.TransferRepositoryPort;
import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.model.Transfer;
import co.edu.tdea.bank.shared.exception.InvalidStateTransitionException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Application service — scans all transfers in {@code PENDING_APPROVAL} and expires
 * those that have exceeded the allowed approval window.
 *
 * <p>Intended to be triggered by a scheduled job. No funds are moved.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>Only transfers in {@code PENDING_APPROVAL} are considered.</li>
 *   <li>A transfer is expired when {@code now - creationDateTime > expirationWindow}.</li>
 *   <li>Domain enforces the {@code PENDING_APPROVAL → EXPIRED} transition.</li>
 *   <li>Each expired transfer is persisted individually.</li>
 *   <li>Each expiration is audited with the reason required by the PDF spec.</li>
 * </ol>
 */
@Service
public class ExpirePendingTransfersService implements ExpirePendingTransfersUseCase {

    private static final String EXPIRATION_REASON =
            "Vencida por falta de aprobación en el tiempo establecido";

    private final TransferRepositoryPort transferRepositoryPort;
    private final AuditLogPort auditLogPort;

    public ExpirePendingTransfersService(TransferRepositoryPort transferRepositoryPort,
                                         AuditLogPort auditLogPort) {
        this.transferRepositoryPort = transferRepositoryPort;
        this.auditLogPort           = auditLogPort;
    }

    @Override
    public List<Long> expirePendingTransfers(LocalDateTime now, Duration expirationWindow) {

        // 1. Load all transfers currently waiting for approval
        List<Transfer> pending = transferRepositoryPort
                .findByStatus(TransferStatus.PENDING_APPROVAL);

        List<Long> expiredIds = new ArrayList<>();

        for (Transfer transfer : pending) {

            // 2. Check whether this transfer has exceeded the expiration window
            if (!transfer.isExpired(now, expirationWindow)) {
                continue;
            }

            // 3. Apply domain state transition — translate IllegalStateException so the
            //    application layer speaks consistently in domain exceptions
            try {
                transfer.expire();
            } catch (IllegalStateException e) {
                throw new InvalidStateTransitionException(
                        "Transfer", transfer.getTransferStatus(), "expire");
            }

            // 4. Persist the expired transfer
            Transfer savedTransfer = transferRepositoryPort.save(transfer);

            // 5. Audit — include the reason mandated by the PDF spec
            auditLogPort.save(
                    "Transfer",
                    String.valueOf(savedTransfer.getTransferId()),
                    "TRANSFER_EXPIRED",
                    savedTransfer.getCreatedBy().getUserId().toString(),
                    now,
                    "motivo=" + EXPIRATION_REASON
                    + ", fechaVencimiento=" + now
                    + ", creationDateTime=" + savedTransfer.getCreationDateTime()
                    + ", sourceAccount=" + savedTransfer.getSourceAccount().getAccountNumber()
                    + ", amount=" + savedTransfer.getAmount()
            );

            expiredIds.add(savedTransfer.getTransferId());
        }

        return expiredIds;
    }
}
