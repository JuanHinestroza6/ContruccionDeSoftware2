package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.enums.UserStatus;
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
class ApproveTransferServiceTest {

    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private BankAccountRepositoryPort bankAccountRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static final LocalDateTime APPROVAL_TIME = LocalDateTime.of(2026, 1, 15, 12, 0);

    private static User supervisor(UUID id) {
        return User.builder()
                .userId(id)
                .fullName("Susan Supervisor")
                .identificationId("U-" + id.toString().substring(0, 8))
                .email("susan@example.com")
                .phone("+57 300 1111111")
                .address("HQ, Medellin")
                .systemRole(SystemRole.COMPANY_SUPERVISOR)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void should_approve_transfer_successfully_when_pending_and_user_is_supervisor() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source      = DomainFixtures.activeBankAccount(client, new BigDecimal("1000000"));
        BankAccount destination = DomainFixtures.activeBankAccount(client, new BigDecimal("500000"));
        User creator = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(source, destination,
                new BigDecimal("200000"), LocalDateTime.of(2026, 1, 15, 10, 0), creator);
        UUID approverId = UUID.randomUUID();
        User approver = supervisor(approverId);

        BigDecimal sourceBefore = source.getCurrentBalance();
        BigDecimal destinationBefore = destination.getCurrentBalance();

        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));
        when(userRepositoryPort.findById(approverId)).thenReturn(Optional.of(approver));
        when(transferRepositoryPort.save(transfer)).thenReturn(transfer);

        ApproveTransferService service = new ApproveTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act
        Transfer result = service.approveTransfer(transfer.getTransferId(),
                approverId, APPROVAL_TIME);

        // Assert
        assertThat(result.getTransferStatus()).isEqualTo(TransferStatus.EXECUTED);
        assertThat(source.getCurrentBalance())
                .isEqualByComparingTo(sourceBefore.subtract(transfer.getAmount()));
        assertThat(destination.getCurrentBalance())
                .isEqualByComparingTo(destinationBefore.add(transfer.getAmount()));
        verify(bankAccountRepositoryPort).save(source);
        verify(bankAccountRepositoryPort).save(destination);
        verify(transferRepositoryPort).save(transfer);
        verify(auditLogPort).save(eq("Transfer"), eq(String.valueOf(transfer.getTransferId())),
                eq("TRANSFER_APPROVED_EXECUTED"), eq(approverId.toString()),
                any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_transfer_not_found() {
        // Arrange
        Long missingId = 9999L;
        when(transferRepositoryPort.findById(missingId)).thenReturn(Optional.empty());

        ApproveTransferService service = new ApproveTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.approveTransfer(missingId,
                UUID.randomUUID(), APPROVAL_TIME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transfer");
        verify(transferRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_approver_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(source, destination, creator);
        UUID approverId = UUID.randomUUID();

        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));
        when(userRepositoryPort.findById(approverId)).thenReturn(Optional.empty());

        ApproveTransferService service = new ApproveTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.approveTransfer(transfer.getTransferId(),
                approverId, APPROVAL_TIME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(transferRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_approver_lacks_required_role() {
        // Arrange — TELLER_EMPLOYEE may not approve transfers
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(source, destination, creator);
        UUID approverId = UUID.randomUUID();
        User teller = User.builder()
                .userId(approverId)
                .fullName("Teller Tim")
                .identificationId("U-TELLER")
                .email("tim@example.com")
                .phone("+57 300 1234567")
                .address("HQ, Medellin")
                .systemRole(SystemRole.TELLER_EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));
        when(userRepositoryPort.findById(approverId)).thenReturn(Optional.of(teller));

        ApproveTransferService service = new ApproveTransferService(
                transferRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.approveTransfer(transfer.getTransferId(),
                approverId, APPROVAL_TIME))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("COMPANY_SUPERVISOR");
        verify(transferRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
