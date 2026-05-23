package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndividualClientTest {

    @Test
    void should_create_client_when_all_fields_valid() {
        // Arrange
        LocalDate birth = LocalDate.now().minusYears(25);

        // Act
        IndividualClient client = IndividualClient.create(
                "1001234567",
                "Jane.Doe@Example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                birth
        );

        // Assert
        assertThat(client).isNotNull();
        assertThat(client.getClientId()).isNotNull();
        assertThat(client.getFullName()).isEqualTo("Jane Doe");
        assertThat(client.getBirthDate()).isEqualTo(birth);
        assertThat(client.getEmail()).isEqualTo("jane.doe@example.com"); // trimmed + lowercased
        assertThat(client.getIdentificationId()).isEqualTo("1001234567");
        assertThat(client.getAge()).isEqualTo(25);
    }

    @Test
    void should_throw_when_birthDate_is_null() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20",
                "Jane Doe",
                null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("birthDate");
    }

    @Test
    void should_throw_when_client_is_under_18() {
        // Arrange
        LocalDate underage = LocalDate.now().minusYears(17);

        // Act / Assert
        assertThatThrownBy(() -> IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20",
                "Jane Doe",
                underage
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 18");
    }

    @Test
    void should_throw_when_birthDate_is_in_the_future() {
        // Arrange
        LocalDate future = LocalDate.now().plusDays(1);

        // Act / Assert
        assertThatThrownBy(() -> IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20",
                "Jane Doe",
                future
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past date");
    }

    @Test
    void should_throw_when_identificationId_is_blank() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> IndividualClient.create(
                "   ",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificationId");
    }

    @Test
    void should_throw_when_email_is_blank() {
        // Email validation requires '@' — a blank string fails the contains check
        assertThatThrownBy(() -> IndividualClient.create(
                "1001234567",
                "   ",
                "+57 300 1234567",
                "Cra 50 # 10-20",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void should_throw_when_phone_is_blank() {
        assertThatThrownBy(() -> IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "   ",
                "Cra 50 # 10-20",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void should_throw_when_fullName_is_blank() {
        assertThatThrownBy(() -> IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20",
                "   ",
                LocalDate.now().minusYears(30)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fullName");
    }

    @Test
    void should_reconstruct_individual_client_with_all_fields() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDate birth = LocalDate.of(1995, 7, 10);

        // Act
        IndividualClient client = IndividualClient.reconstruct(
                id,
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                birth
        );

        // Assert
        assertThat(client.getClientId()).isEqualTo(id);
        assertThat(client.getIdentificationId()).isEqualTo("1001234567");
        assertThat(client.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(client.getPhone()).isEqualTo("+57 300 1234567");
        assertThat(client.getAddress()).isEqualTo("Cra 50 # 10-20, Medellin");
        assertThat(client.getFullName()).isEqualTo("Jane Doe");
        assertThat(client.getBirthDate()).isEqualTo(birth);
    }

    @Test
    void should_use_valid_fixture_to_build_individual_client() {
        // Arrange / Act
        IndividualClient client = DomainFixtures.validIndividualClient();

        // Assert
        assertThat(client.getAge()).isGreaterThanOrEqualTo(18);
        assertThat(client.getClientId()).isNotNull();
    }
}
