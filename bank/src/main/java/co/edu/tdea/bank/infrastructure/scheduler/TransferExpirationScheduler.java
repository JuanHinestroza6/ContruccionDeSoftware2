package co.edu.tdea.bank.infrastructure.scheduler;

import co.edu.tdea.bank.domain.ports.in.ExpirePendingTransfersUseCase;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that expires pending transfers whose approval window has elapsed.
 */
@Component
public class TransferExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransferExpirationScheduler.class);

    @Value("${bank.transfers.expiration-window-minutes}")
    private long expirationWindowMinutes;

    private final ExpirePendingTransfersUseCase expirePendingTransfers;

    public TransferExpirationScheduler(ExpirePendingTransfersUseCase expirePendingTransfers) {
        this.expirePendingTransfers = expirePendingTransfers;
    }

    @PostConstruct
    void validateConfig() {
        if (expirationWindowMinutes <= 0) {
            throw new IllegalStateException(
                "bank.transfers.expiration-window-minutes must be > 0, got: " + expirationWindowMinutes);
        }
    }

    @Scheduled(fixedRateString = "${bank.transfers.expiration-job-rate-ms}")
    public void expirePendingTransfers() {
        log.debug("Running scheduled job â€” window={} minutes", expirationWindowMinutes);
        try {
            List<Long> expiredIds = expirePendingTransfers.expirePendingTransfers(
                LocalDateTime.now(),
                Duration.ofMinutes(expirationWindowMinutes)
            );
            log.debug("Scheduled job completed â€” window={} minutes, expired={} transfer(s)",
                expirationWindowMinutes, expiredIds.size());
        } catch (Exception e) {
            log.error("Error during scheduled expiration of pending transfers", e);
        }
    }
}
