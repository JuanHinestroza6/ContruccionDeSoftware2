package co.edu.tdea.bank.domain.ports.in;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Input port — scans all pending transfers and expires those that have exceeded
 * the allowed approval window.
 *
 * <p>Intended to be triggered by a scheduled job, not by an end-user action.
 */
public interface ExpirePendingTransfersUseCase {

    /**
     * @param now              current date-time used as the expiration reference
     * @param expirationWindow maximum time a transfer may remain in {@code PENDING_APPROVAL}
     * @return list of transfer IDs that were transitioned to {@code EXPIRED}
     */
    List<Long> expirePendingTransfers(LocalDateTime now, Duration expirationWindow);
}
