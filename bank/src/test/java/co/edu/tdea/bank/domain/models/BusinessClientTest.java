package co.edu.tdea.bank.domain.models;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessClientTest {

    @Test
    void should_create_business_client_when_all_fields_valid() {
        // Arrange / Act
        BusinessClient client = BusinessClient.create(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3, Medellin",
                "  ACME S.A.S.  ",
                "  John Doe  "
        );

        // Assert
        assertThat(client).isNotNull();
        assertThat(client.getClientId()).isNotNull();
        assertThat(client.getCompanyName()).isEqualTo("ACME S.A.S."); // trimmed
        assertThat(client.getLegalRepresentative()).isEqualTo("John Doe"); // trimmed
        assertThat(client.getEmail()).isEqualTo("contact@acme.co");
        assertThat(client.getIdentificationId()).isEqualTo("900123456-7");
    }

    @Test
    void should_throw_when_companyName_is_blank() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> BusinessClient.create(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3",
                "   ",
                "John Doe"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companyName");
    }

    @Test
    void should_throw_when_legalRepresentative_is_blank() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> BusinessClient.create(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3",
                "ACME S.A.S.",
                "   "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legalRepresentative");
    }

    @Test
    void should_throw_when_email_is_invalid() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> BusinessClient.create(
                "900123456-7",
                "not-an-email",
                "+57 604 1234567",
                "Cl 1 # 2-3",
                "ACME S.A.S.",
                "John Doe"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void should_update_legal_representative_when_new_value_is_valid() {
        // Arrange
        BusinessClient client = BusinessClient.create(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3",
                "ACME S.A.S.",
                "John Doe"
        );

        // Act
        client.updateLegalRepresentative("  Alice Wonderland  ");

        // Assert
        assertThat(client.getLegalRepresentative()).isEqualTo("Alice Wonderland");
    }

    @Test
    void should_throw_when_updating_legal_representative_with_blank() {
        // Arrange
        BusinessClient client = BusinessClient.create(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3",
                "ACME S.A.S.",
                "John Doe"
        );

        // Act / Assert
        assertThatThrownBy(() -> client.updateLegalRepresentative("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legalRepresentative");
    }

    @Test
    void should_reconstruct_business_client_with_all_fields() {
        // Arrange
        UUID id = UUID.randomUUID();

        // Act
        BusinessClient client = BusinessClient.reconstruct(
                id,
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3, Medellin",
                "ACME S.A.S.",
                "John Doe"
        );

        // Assert
        assertThat(client.getClientId()).isEqualTo(id);
        assertThat(client.getIdentificationId()).isEqualTo("900123456-7");
        assertThat(client.getEmail()).isEqualTo("contact@acme.co");
        assertThat(client.getCompanyName()).isEqualTo("ACME S.A.S.");
        assertThat(client.getLegalRepresentative()).isEqualTo("John Doe");
    }
}
