package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindClientServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;

    @Test
    void should_return_client_when_found_by_id() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        when(clientRepositoryPort.findById(client.getClientId())).thenReturn(Optional.of(client));

        FindClientService service = new FindClientService(clientRepositoryPort);

        // Act
        Client result = service.findById(client.getClientId());

        // Assert
        assertThat(result).isEqualTo(client);
    }

    @Test
    void should_throw_ResourceNotFoundException_when_client_id_not_found() {
        // Arrange
        UUID missing = UUID.randomUUID();
        when(clientRepositoryPort.findById(missing)).thenReturn(Optional.empty());

        FindClientService service = new FindClientService(clientRepositoryPort);

        // Act + Assert
        assertThatThrownBy(() -> service.findById(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client");
    }

    @Test
    void should_return_client_when_found_by_identification_id() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        when(clientRepositoryPort.findByIdentificationId(client.getIdentificationId()))
                .thenReturn(Optional.of(client));

        FindClientService service = new FindClientService(clientRepositoryPort);

        // Act
        Client result = service.findByIdentificationId(client.getIdentificationId());

        // Assert
        assertThat(result).isEqualTo(client);
    }

    @Test
    void should_throw_ResourceNotFoundException_when_identification_id_not_found() {
        // Arrange
        String missingId = "NOT-FOUND";
        when(clientRepositoryPort.findByIdentificationId(missingId)).thenReturn(Optional.empty());

        FindClientService service = new FindClientService(clientRepositoryPort);

        // Act + Assert
        assertThatThrownBy(() -> service.findByIdentificationId(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("identification ID");
    }
}
