package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransferServiceTest {

    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private BankAccountRepositoryPort bankAccountRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static final LocalDateTime CREATION_TIME = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Test
    void should_create_transfer_as_pending_when_amount_exceeds_threshold() {
        // Arrange — amount (10_000_000) is strictly greater than threshold (5_000_000)
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source      = DomainFixtures.activeBankAccount(client, new BigDecimal("20000000"));
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator    = DomainFixtures.validUser();
        BigDecimal amount    = new BigDecimal("10000000");
        BigDecimal threshold = new BigDecimal("5000000");

        // savedTransfer simulates what the persistence adapter returns after DB assigns the ID
        Transfer savedTransfer = Transfer.reconstruct(999L, source, destination, amount,
                CREATION_TIME, creator, null, TransferStatus.PENDING_APPROVAL, null);

        when(userRepositoryPort.findById(creator.getUserId())).thenReturn(Optional.of(creator));
        when(bankAccountRepositoryPort.findByAccountNumber(source.getAccountNumber()))
                .thenReturn(Optional.of(source));
        when(bankAccountRepositoryPort.findByAccountNumber(destination.getAccountNumber()))
                .thenReturn(Optional.of(destination));
        when(transferRepositoryPort.save(any(Transfer.class))).thenReturn(savedTransfer);

        CreateTransferService service = new CreateTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act
        Transfer result = service.createTransfer(source.getAccountNumber(),
                destination.getAccountNumber(), amount,
                creator.getUserId(), CREATION_TIME, threshold);

        // Assert — transfer stored in PENDING_APPROVAL, no balances moved
        assertThat(result).isSameAs(savedTransfer);
        assertThat(result.getTransferStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
        verify(transferRepositoryPort).save(any(Transfer.class));
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort).save(eq("Transfer"), eq("999"),
                eq("TRANSFER_PENDING_APPROVAL"), eq(creator.getUserId().toString()),
                any(), anyString());
    }

    @Test
    void should_create_transfer_as_executed_when_amount_below_threshold() {
        // Arrange — amount (100_000) is well below threshold (5_000_000)
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source      = DomainFixtures.activeBankAccount(client, new BigDecimal("1000000"));
        BankAccount destination = DomainFixtures.activeBankAccount(client, new BigDecimal("500000"));
        User creator    = DomainFixtures.validUser();
        BigDecimal amount    = new BigDecimal("100000");
        BigDecimal threshold = new BigDecimal("5000000");

        BigDecimal sourceBalanceBefore      = source.getCurrentBalance();
        BigDecimal destinationBalanceBefore = destination.getCurrentBalance();

        // savedTransfer simulates what the persistence adapter returns after DB assigns the ID
        Transfer savedTransfer = Transfer.reconstruct(888L, source, destination, amount,
                CREATION_TIME, creator, CREATION_TIME, TransferStatus.EXECUTED, creator);

        when(userRepositoryPort.findById(creator.getUserId())).thenReturn(Optional.of(creator));
        when(bankAccountRepositoryPort.findByAccountNumber(source.getAccountNumber()))
                .thenReturn(Optional.of(source));
        when(bankAccountRepositoryPort.findByAccountNumber(destination.getAccountNumber()))
                .thenReturn(Optional.of(destination));
        when(transferRepositoryPort.save(any(Transfer.class))).thenReturn(savedTransfer);

        CreateTransferService service = new CreateTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act
        Transfer result = service.createTransfer(source.getAccountNumber(),
                destination.getAccountNumber(), amount,
                creator.getUserId(), CREATION_TIME, threshold);

        // Assert — funds moved + status EXECUTED + audit captured
        assertThat(result).isSameAs(savedTransfer);
        assertThat(result.getTransferStatus()).isEqualTo(TransferStatus.EXECUTED);
        assertThat(source.getCurrentBalance())
                .isEqualByComparingTo(sourceBalanceBefore.subtract(amount));
        assertThat(destination.getCurrentBalance())
                .isEqualByComparingTo(destinationBalanceBefore.add(amount));
        verify(bankAccountRepositoryPort).save(source);
        verify(bankAccountRepositoryPort).save(destination);
        verify(transferRepositoryPort).save(any(Transfer.class));
        verify(auditLogPort).save(eq("Transfer"), eq("888"),
                eq("TRANSFER_EXECUTED"), eq(creator.getUserId().toString()),
                any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_creator_user_not_found() {
        // Arrange
        UUID missingUserId = UUID.randomUUID();
        when(userRepositoryPort.findById(missingUserId)).thenReturn(Optional.empty());

        CreateTransferService service = new CreateTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.createTransfer("ACC-A", "ACC-B",
                new BigDecimal("1000"), missingUserId, CREATION_TIME,
                new BigDecimal("5000000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(transferRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_source_account_not_found() {
        // Arrange
        User creator = DomainFixtures.validUser();
        when(userRepositoryPort.findById(creator.getUserId())).thenReturn(Optional.of(creator));
        when(bankAccountRepositoryPort.findByAccountNumber("ACC-MISSING-SRC"))
                .thenReturn(Optional.empty());

        CreateTransferService service = new CreateTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.createTransfer("ACC-MISSING-SRC", "ACC-B",
                new BigDecimal("1000"), creator.getUserId(), CREATION_TIME,
                new BigDecimal("5000000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount");
        verify(transferRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_destination_account_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        when(userRepositoryPort.findById(creator.getUserId())).thenReturn(Optional.of(creator));
        when(bankAccountRepositoryPort.findByAccountNumber(source.getAccountNumber()))
                .thenReturn(Optional.of(source));
        when(bankAccountRepositoryPort.findByAccountNumber("ACC-MISSING-DST"))
                .thenReturn(Optional.empty());

        CreateTransferService service = new CreateTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.createTransfer(source.getAccountNumber(),
                "ACC-MISSING-DST", new BigDecimal("1000"), creator.getUserId(),
                CREATION_TIME, new BigDecimal("5000000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount");
        verify(transferRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
