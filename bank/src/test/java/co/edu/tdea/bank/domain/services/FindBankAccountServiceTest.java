package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindBankAccountServiceTest {

    @Mock private BankAccountRepositoryPort bankAccountRepositoryPort;

    @Test
    void should_return_account_when_found_by_account_number() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount account = DomainFixtures.activeBankAccount(client);
        when(bankAccountRepositoryPort.findByAccountNumber(account.getAccountNumber()))
                .thenReturn(Optional.of(account));

        FindBankAccountService service = new FindBankAccountService(bankAccountRepositoryPort);

        // Act
        BankAccount result = service.findByAccountNumber(account.getAccountNumber());

        // Assert
        assertThat(result).isEqualTo(account);
    }

    @Test
    void should_throw_ResourceNotFoundException_when_account_number_not_found() {
        // Arrange
        String missing = "ACC-MISSING";
        when(bankAccountRepositoryPort.findByAccountNumber(missing)).thenReturn(Optional.empty());

        FindBankAccountService service = new FindBankAccountService(bankAccountRepositoryPort);

        // Act + Assert
        assertThatThrownBy(() -> service.findByAccountNumber(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount");
    }

    @Test
    void should_return_accounts_for_client_when_queried_by_client_id() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount account = DomainFixtures.activeBankAccount(client);
        UUID clientId = client.getClientId();
        when(bankAccountRepositoryPort.findByClientId(clientId))
                .thenReturn(List.of(account));

        FindBankAccountService service = new FindBankAccountService(bankAccountRepositoryPort);

        // Act
        List<BankAccount> result = service.findByClientId(clientId);

        // Assert
        assertThat(result).containsExactly(account);
    }
}
