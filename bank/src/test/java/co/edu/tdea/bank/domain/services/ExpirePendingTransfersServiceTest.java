package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the scheduled job that expires PENDING_APPROVAL transfers.
 *
 * <p>The service receives {@code now} and {@code expirationWindow} as method parameters,
 * so no Clock injection / production refactor is required for deterministic testing.
 */
@ExtendWith(MockitoExtension.class)
class ExpirePendingTransfersServiceTest {

    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static final LocalDateTime CREATION_TIME = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final Duration EXPIRATION_WINDOW  = Duration.ofMinutes(60);

    @Test
    void should_expire_transfer_when_created_more_than_60_minutes_ago() {
        // Arrange — created at 10:00, "now" is 11:10 (70 minutes later)
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer expiringTransfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100000"), CREATION_TIME, creator);
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 11, 10);

        when(transferRepositoryPort.findByStatus(TransferStatus.PENDING_APPROVAL))
                .thenReturn(List.of(expiringTransfer));
        when(transferRepositoryPort.save(expiringTransfer)).thenReturn(expiringTransfer);

        ExpirePendingTransfersService service = new ExpirePendingTransfersService(
                transferRepositoryPort, auditLogPort);

        // Act
        List<Long> expiredIds = service.expirePendingTransfers(now, EXPIRATION_WINDOW);

        // Assert
        assertThat(expiredIds).containsExactly(expiringTransfer.getTransferId());
        assertThat(expiringTransfer.getTransferStatus()).isEqualTo(TransferStatus.EXPIRED);
        verify(transferRepositoryPort).save(expiringTransfer);
        verify(auditLogPort).save(eq("Transfer"),
                eq(String.valueOf(expiringTransfer.getTransferId())),
                eq("TRANSFER_EXPIRED"),
                eq(creator.getUserId().toString()),
                eq(now), anyString());
    }

    @Test
    void should_not_expire_transfer_when_created_less_than_60_minutes_ago() {
        // Arrange — created at 10:00, "now" is 10:30 (30 minutes later)
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer freshTransfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100000"), CREATION_TIME, creator);
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 30);

        when(transferRepositoryPort.findByStatus(TransferStatus.PENDING_APPROVAL))
                .thenReturn(List.of(freshTransfer));

        ExpirePendingTransfersService service = new ExpirePendingTransfersService(
                transferRepositoryPort, auditLogPort);

        // Act
        List<Long> expiredIds = service.expirePendingTransfers(now, EXPIRATION_WINDOW);

        // Assert
        assertThat(expiredIds).isEmpty();
        assertThat(freshTransfer.getTransferStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
        verify(transferRepositoryPort, never()).save(any());
        verifyNoInteractions(auditLogPort);
    }

    @Test
    void should_expire_only_old_transfers_when_pending_list_is_mixed() {
        // Arrange — one transfer created at 10:00 (will expire), another at 11:00 (still fresh)
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source1 = DomainFixtures.activeBankAccount(client);
        BankAccount destination1 = DomainFixtures.activeBankAccount(client);
        BankAccount source2 = DomainFixtures.activeBankAccount(client);
        BankAccount destination2 = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();

        Transfer oldTransfer = DomainFixtures.validPendingTransfer(source1, destination1,
                new BigDecimal("100000"), LocalDateTime.of(2026, 1, 15, 10, 0), creator);
        Transfer freshTransfer = DomainFixtures.validPendingTransfer(source2, destination2,
                new BigDecimal("200000"), LocalDateTime.of(2026, 1, 15, 11, 0), creator);
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 11, 30);

        when(transferRepositoryPort.findByStatus(TransferStatus.PENDING_APPROVAL))
                .thenReturn(List.of(oldTransfer, freshTransfer));
        when(transferRepositoryPort.save(oldTransfer)).thenReturn(oldTransfer);

        ExpirePendingTransfersService service = new ExpirePendingTransfersService(
                transferRepositoryPort, auditLogPort);

        // Act
        List<Long> expiredIds = service.expirePendingTransfers(now, EXPIRATION_WINDOW);

        // Assert
        assertThat(expiredIds).containsExactly(oldTransfer.getTransferId());
        assertThat(oldTransfer.getTransferStatus()).isEqualTo(TransferStatus.EXPIRED);
        assertThat(freshTransfer.getTransferStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
        verify(transferRepositoryPort, times(1)).save(any());
        verify(transferRepositoryPort).save(oldTransfer);
        verify(auditLogPort, times(1)).save(anyString(), anyString(), anyString(),
                anyString(), any(), anyString());
        verify(auditLogPort).save(eq("Transfer"),
                eq(String.valueOf(oldTransfer.getTransferId())),
                eq("TRANSFER_EXPIRED"), eq(creator.getUserId().toString()),
                eq(now), anyString());
    }

    @Test
    void should_return_empty_list_when_no_pending_transfers_exist() {
        // Arrange
        when(transferRepositoryPort.findByStatus(TransferStatus.PENDING_APPROVAL))
                .thenReturn(List.of());

        ExpirePendingTransfersService service = new ExpirePendingTransfersService(
                transferRepositoryPort, auditLogPort);

        // Act
        List<Long> expiredIds = service.expirePendingTransfers(
                LocalDateTime.of(2026, 1, 15, 12, 0), EXPIRATION_WINDOW);

        // Assert
        assertThat(expiredIds).isEmpty();
        verify(transferRepositoryPort, never()).save(any());
        verifyNoInteractions(auditLogPort);
    }
}
