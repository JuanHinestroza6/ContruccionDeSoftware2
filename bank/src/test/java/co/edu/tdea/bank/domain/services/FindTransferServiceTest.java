package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;
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
class FindTransferServiceTest {

    @Mock private TransferRepositoryPort transferRepositoryPort;

    private Transfer buildTransfer() {
        IndividualClient client = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        User creator = DomainFixtures.validUser();
        return DomainFixtures.validPendingTransfer(source, destination, creator);
    }

    @Test
    void should_return_transfer_when_found_by_id() {
        // Arrange
        Transfer transfer = buildTransfer();
        when(transferRepositoryPort.findById(transfer.getTransferId()))
                .thenReturn(Optional.of(transfer));

        FindTransferService service = new FindTransferService(transferRepositoryPort);

        // Act
        Transfer result = service.findById(transfer.getTransferId());

        // Assert
        assertThat(result).isEqualTo(transfer);
    }

    @Test
    void should_throw_ResourceNotFoundException_when_transfer_id_not_found() {
        // Arrange
        Long missing = 9999L;
        when(transferRepositoryPort.findById(missing)).thenReturn(Optional.empty());

        FindTransferService service = new FindTransferService(transferRepositoryPort);

        // Act + Assert
        assertThatThrownBy(() -> service.findById(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transfer");
    }

    @Test
    void should_return_transfers_by_source_account_number() {
        // Arrange
        Transfer transfer = buildTransfer();
        String sourceAccountNumber = transfer.getSourceAccount().getAccountNumber();
        when(transferRepositoryPort.findBySourceAccountNumber(sourceAccountNumber))
                .thenReturn(List.of(transfer));

        FindTransferService service = new FindTransferService(transferRepositoryPort);

        // Act
        List<Transfer> result = service.findBySourceAccountNumber(sourceAccountNumber);

        // Assert
        assertThat(result).containsExactly(transfer);
    }

    @Test
    void should_return_transfers_by_created_by_user_id() {
        // Arrange
        Transfer transfer = buildTransfer();
        UUID userId = transfer.getCreatedBy().getUserId();
        when(transferRepositoryPort.findByCreatedByUserId(userId))
                .thenReturn(List.of(transfer));

        FindTransferService service = new FindTransferService(transferRepositoryPort);

        // Act
        List<Transfer> result = service.findByCreatedByUserId(userId);

        // Assert
        assertThat(result).containsExactly(transfer);
    }
}
