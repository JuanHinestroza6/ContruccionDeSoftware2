package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.BusinessException;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterIndividualClientServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    @Test
    void should_register_individual_client_successfully_when_identification_and_email_unique() {
        // Arrange
        String identificationId = "1001234567";
        String email = "alice@example.com";
        LocalDate birthDate = LocalDate.now().minusYears(30);

        when(clientRepositoryPort.existsByIdentificationId(identificationId)).thenReturn(false);
        when(clientRepositoryPort.existsByEmail(email)).thenReturn(false);
        when(clientRepositoryPort.save(any(IndividualClient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterIndividualClientService service = new RegisterIndividualClientService(
                clientRepositoryPort, auditLogPort);

        // Act
        IndividualClient saved = service.registerIndividualClient(identificationId, email,
                "+57 300 1234567", "Cra 50 # 10-20", "Alice Wonder", birthDate);

        // Assert
        assertThat(saved.getIdentificationId()).isEqualTo(identificationId);
        assertThat(saved.getEmail()).isEqualTo(email);
        verify(clientRepositoryPort).save(any(IndividualClient.class));
        verify(auditLogPort).save(eq("IndividualClient"), anyString(),
                eq("REGISTER_INDIVIDUAL_CLIENT"), eq("SYSTEM"),
                any(), eq(null));
    }

    @Test
    void should_throw_BusinessException_when_identificationId_already_exists() {
        // Arrange
        when(clientRepositoryPort.existsByIdentificationId("1001234567")).thenReturn(true);

        RegisterIndividualClientService service = new RegisterIndividualClientService(
                clientRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerIndividualClient("1001234567",
                "alice@example.com", "+57 300 1234567", "Cra 50",
                "Alice", LocalDate.now().minusYears(25)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("identification ID");
        verify(clientRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_BusinessException_when_email_already_exists() {
        // Arrange
        when(clientRepositoryPort.existsByIdentificationId("1001234567")).thenReturn(false);
        when(clientRepositoryPort.existsByEmail("alice@example.com")).thenReturn(true);

        RegisterIndividualClientService service = new RegisterIndividualClientService(
                clientRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerIndividualClient("1001234567",
                "alice@example.com", "+57 300 1234567", "Cra 50",
                "Alice", LocalDate.now().minusYears(25)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");
        verify(clientRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
