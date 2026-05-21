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
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RejectTransferServiceTest {

    @Mock private TransferRepositoryPort transferRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static User businessAdmin(UUID id) {
        return User.builder()
                .userId(id)
                .fullName("Bea Boss")
                .identificationId("U-" + id.toString().substring(0, 8))
                .email("bea@example.com")
                .phone("+57 300 9999999")
                .address("HQ, Medellin")
                .systemRole(SystemRole.BUSINESS_ADMIN)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void should_reject_transfer_successfully_when_pending_and_user_is_business_admin() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(source, destination, creator);
        UUID rejectorId = UUID.randomUUID();
        User rejector = businessAdmin(rejectorId);

        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));
        when(userRepositoryPort.findById(rejectorId)).thenReturn(Optional.of(rejector));
        when(transferRepositoryPort.save(transfer)).thenReturn(transfer);

        RejectTransferService service = new RejectTransferService(
                transferRepositoryPort, userRepositoryPort, auditLogPort);

        // Act
        Transfer result = service.rejectTransfer(transfer.getTransferId(), rejectorId);

        // Assert
        assertThat(result.getTransferStatus()).isEqualTo(TransferStatus.REJECTED);
        verify(transferRepositoryPort).save(transfer);
        verify(auditLogPort).save(eq("Transfer"), eq(String.valueOf(transfer.getTransferId())),
                eq("TRANSFER_REJECTED"), eq(rejectorId.toString()),
                any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_transfer_not_found() {
        // Arrange
        Long missingId = 9999L;
        when(transferRepositoryPort.findById(missingId)).thenReturn(Optional.empty());

        RejectTransferService service = new RejectTransferService(
                transferRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.rejectTransfer(missingId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transfer");
        verify(transferRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_rejecter_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(source, destination, creator);
        UUID rejectorId = UUID.randomUUID();

        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));
        when(userRepositoryPort.findById(rejectorId)).thenReturn(Optional.empty());

        RejectTransferService service = new RejectTransferService(
                transferRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.rejectTransfer(transfer.getTransferId(), rejectorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(transferRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_rejecter_lacks_required_role() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(source, destination, creator);
        UUID rejectorId = UUID.randomUUID();
        User opUser = User.builder()
                .userId(rejectorId)
                .fullName("Ollie Operator")
                .identificationId("U-OPER")
                .email("ollie@example.com")
                .phone("+57 300 9999998")
                .address("HQ, Medellin")
                .systemRole(SystemRole.COMPANY_OPERATOR)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));
        when(userRepositoryPort.findById(rejectorId)).thenReturn(Optional.of(opUser));

        RejectTransferService service = new RejectTransferService(
                transferRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.rejectTransfer(transfer.getTransferId(), rejectorId))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("COMPANY_SUPERVISOR");
        verify(transferRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
