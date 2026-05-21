package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.BusinessException;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class OpenBankAccountServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private BankAccountRepositoryPort bankAccountRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static User userWithStatus(UUID id, UserStatus status) {
        return User.builder()
                .userId(id)
                .fullName("Carl Client")
                .identificationId("U-" + id.toString().substring(0, 8))
                .email("carl@example.com")
                .phone("+57 300 5555555")
                .address("Cl 1 # 2-3")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .userStatus(status)
                .build();
    }

    @Test
    void should_open_account_successfully_when_client_and_active_user_exist_and_number_unique() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User active = userWithStatus(UUID.randomUUID(), UserStatus.ACTIVE);
        String accountNumber = "ACC-NEW-001";

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(active));
        when(bankAccountRepositoryPort.existsByAccountNumber(accountNumber)).thenReturn(false);
        when(bankAccountRepositoryPort.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OpenBankAccountService service = new OpenBankAccountService(
                clientRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act
        BankAccount result = service.openAccount(clientId, accountNumber,
                AccountType.SAVINGS, new BigDecimal("500000"), CurrencyType.COP);

        // Assert
        assertThat(result.getAccountNumber()).isEqualTo(accountNumber);
        assertThat(result.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("500000"));
        verify(bankAccountRepositoryPort).save(any(BankAccount.class));
        verify(auditLogPort).save(eq("BankAccount"), eq(accountNumber),
                eq("ACCOUNT_OPENED"), eq(active.getUserId().toString()),
                any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_client_not_found() {
        // Arrange
        UUID missingClientId = UUID.randomUUID();
        when(clientRepositoryPort.findById(missingClientId)).thenReturn(Optional.empty());

        OpenBankAccountService service = new OpenBankAccountService(
                clientRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.openAccount(missingClientId, "ACC-001",
                AccountType.SAVINGS, new BigDecimal("100000"), CurrencyType.COP))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client");
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_linked_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.empty());

        OpenBankAccountService service = new OpenBankAccountService(
                clientRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.openAccount(clientId, "ACC-001",
                AccountType.SAVINGS, new BigDecimal("100000"), CurrencyType.COP))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("System user linked to client");
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_user_is_inactive() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User inactive = userWithStatus(UUID.randomUUID(), UserStatus.INACTIVE);

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(inactive));

        OpenBankAccountService service = new OpenBankAccountService(
                clientRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.openAccount(clientId, "ACC-001",
                AccountType.SAVINGS, new BigDecimal("100000"), CurrencyType.COP))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("INACTIVE");
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_user_is_blocked() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User blocked = userWithStatus(UUID.randomUUID(), UserStatus.BLOCKED);

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(blocked));

        OpenBankAccountService service = new OpenBankAccountService(
                clientRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.openAccount(clientId, "ACC-001",
                AccountType.SAVINGS, new BigDecimal("100000"), CurrencyType.COP))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("BLOCKED");
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_BusinessException_when_account_number_already_exists() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User active = userWithStatus(UUID.randomUUID(), UserStatus.ACTIVE);
        String duplicateAccountNumber = "ACC-DUPLICATE";

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(active));
        when(bankAccountRepositoryPort.existsByAccountNumber(duplicateAccountNumber))
                .thenReturn(true);

        OpenBankAccountService service = new OpenBankAccountService(
                clientRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.openAccount(clientId, duplicateAccountNumber,
                AccountType.SAVINGS, new BigDecimal("100000"), CurrencyType.COP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
