package co.edu.tdea.bank.infrastructure.adapter.sql;

import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.ClientRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BusinessClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.IndividualClientJpaRepository;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration tests for {@link ClientRepositoryAdapter}.
 *
 * <p>Uses H2 (MySQL compatibility mode) as the embedded engine so DB-level
 * constraints (unique identification_id, unique email, JOINED inheritance
 * discriminator) are exercised exactly as in production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
@Import(ClientRepositoryAdapter.class)
class ClientRepositoryAdapterIT {

    private final ClientRepositoryAdapter adapter;
    private final EntityManager em;

    @Autowired
    ClientRepositoryAdapterIT(ClientJpaRepository clientJpa,
                              IndividualClientJpaRepository individualJpa,
                              BusinessClientJpaRepository businessJpa,
                              EntityManager em) {
        this.adapter = new ClientRepositoryAdapter(clientJpa, individualJpa, businessJpa);
        this.em = em;
    }

    @Test
    void should_PersistAndReturnSavedClient_when_SavingIndividualClient() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();

        // Act
        Client saved = adapter.save(client);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved).isInstanceOf(IndividualClient.class);
        IndividualClient individual = (IndividualClient) saved;
        assertThat(individual.getClientId()).isEqualTo(client.getClientId());
        assertThat(individual.getIdentificationId()).isEqualTo(client.getIdentificationId());
        assertThat(individual.getEmail()).isEqualTo(client.getEmail());
        assertThat(individual.getFullName()).isEqualTo(client.getFullName());
        assertThat(individual.getBirthDate()).isEqualTo(client.getBirthDate());
    }

    @Test
    void should_PersistAndReturnSavedClient_when_SavingBusinessClient() {
        // Arrange
        BusinessClient client = DomainFixtures.validBusinessClient();

        // Act
        Client saved = adapter.save(client);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved).isInstanceOf(BusinessClient.class);
        BusinessClient business = (BusinessClient) saved;
        assertThat(business.getClientId()).isEqualTo(client.getClientId());
        assertThat(business.getCompanyName()).isEqualTo(client.getCompanyName());
        assertThat(business.getLegalRepresentative()).isEqualTo(client.getLegalRepresentative());
    }

    @Test
    void should_ReturnClient_when_FindByIdWithExistingIndividualId() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        adapter.save(client);
        em.flush();
        em.clear();

        // Act
        Optional<Client> found = adapter.findById(client.getClientId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(IndividualClient.class);
        assertThat(found.get().getClientId()).isEqualTo(client.getClientId());
    }

    @Test
    void should_ReturnEmpty_when_FindByIdWithUnknownId() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act
        Optional<Client> found = adapter.findById(unknownId);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void should_ReturnClient_when_FindByIdentificationIdMatches() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        adapter.save(client);
        em.flush();
        em.clear();

        // Act
        Optional<Client> found = adapter.findByIdentificationId(client.getIdentificationId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getIdentificationId()).isEqualTo(client.getIdentificationId());
    }

    @Test
    void should_ReturnEmpty_when_FindByIdentificationIdNotMatching() {
        // Arrange
        String unknown = "NON-EXISTENT-ID";

        // Act
        Optional<Client> found = adapter.findByIdentificationId(unknown);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void should_ReturnClient_when_FindByEmailMatches() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        adapter.save(client);
        em.flush();
        em.clear();

        // Act
        Optional<Client> found = adapter.findByEmail(client.getEmail());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(client.getEmail());
    }

    @Test
    void should_ReturnTrue_when_ExistsByIdentificationIdMatches() {
        // Arrange
        BusinessClient client = DomainFixtures.validBusinessClient();
        adapter.save(client);
        em.flush();

        // Act
        boolean exists = adapter.existsByIdentificationId(client.getIdentificationId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void should_ReturnFalse_when_ExistsByIdentificationIdDoesNotMatch() {
        // Act
        boolean exists = adapter.existsByIdentificationId("UNKNOWN-99999");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void should_ReturnTrue_when_ExistsByEmailMatches() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        adapter.save(client);
        em.flush();

        // Act
        boolean exists = adapter.existsByEmail(client.getEmail());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void should_ListAllIndividualClients_when_BothSubtypesExist() {
        // Arrange
        IndividualClient individual1 = DomainFixtures.validIndividualClient();
        IndividualClient individual2 = IndividualClient.create(
                "1002222222", "another.person@example.com",
                "+57 300 0000001", "Cra 60 # 11-22, Medellin",
                "John Smith", individual1.getBirthDate());
        BusinessClient business = DomainFixtures.validBusinessClient();
        adapter.save(individual1);
        adapter.save(individual2);
        adapter.save(business);
        em.flush();
        em.clear();

        // Act
        List<IndividualClient> all = adapter.findAllIndividualClients();

        // Assert
        assertThat(all).hasSize(2);
        assertThat(all).extracting(IndividualClient::getIdentificationId)
                .containsExactlyInAnyOrder(
                        individual1.getIdentificationId(),
                        individual2.getIdentificationId());
    }

    @Test
    void should_ListAllBusinessClients_when_BothSubtypesExist() {
        // Arrange
        IndividualClient individual = DomainFixtures.validIndividualClient();
        BusinessClient business = DomainFixtures.validBusinessClient();
        adapter.save(individual);
        adapter.save(business);
        em.flush();
        em.clear();

        // Act
        List<BusinessClient> all = adapter.findAllBusinessClients();

        // Assert
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getIdentificationId()).isEqualTo(business.getIdentificationId());
    }

    @Test
    void should_RejectDuplicate_when_SavingTwoClientsWithSameIdentificationId() {
        // Arrange
        IndividualClient first = DomainFixtures.validIndividualClient();
        adapter.save(first);
        em.flush();
        IndividualClient duplicate = IndividualClient.create(
                first.getIdentificationId(),
                "different.email@example.com",
                "+57 300 7777777",
                "Different Address 123",
                "Other Name",
                first.getBirthDate());

        // Act + Assert
        // Hibernate raises ConstraintViolationException; Spring's exception
        // translator only wraps it into DataIntegrityViolationException when
        // the flush goes through the Spring-proxied repository — here we are
        // explicitly flushing the EntityManager so accept either type.
        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            em.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class,
                ConstraintViolationException.class);
    }

    @Test
    void should_RejectDuplicate_when_SavingTwoClientsWithSameEmail() {
        // Arrange
        IndividualClient first = DomainFixtures.validIndividualClient();
        adapter.save(first);
        em.flush();
        IndividualClient duplicate = IndividualClient.create(
                "9999999999",
                first.getEmail(),
                "+57 300 7777777",
                "Different Address 123",
                "Other Name",
                first.getBirthDate());

        // Act + Assert
        // See note in identification-id test above re: exception translation.
        assertThatThrownBy(() -> {
            adapter.save(duplicate);
            em.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class,
                ConstraintViolationException.class);
    }
}
