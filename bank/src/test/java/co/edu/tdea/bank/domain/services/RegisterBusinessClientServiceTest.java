package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.BusinessException;
import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterBusinessClientServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    @Test
    void should_register_business_client_successfully_when_NIT_and_email_unique() {
        // Arrange
        String nit = "900123456-7";
        String email = "contact@acme.co";
        when(clientRepositoryPort.existsByIdentificationId(nit)).thenReturn(false);
        when(clientRepositoryPort.existsByEmail(email)).thenReturn(false);
        when(clientRepositoryPort.save(any(BusinessClient.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterBusinessClientService service = new RegisterBusinessClientService(
                clientRepositoryPort, auditLogPort);

        // Act
        BusinessClient saved = service.registerBusinessClient(nit, email,
                "+57 604 1234567", "Cl 1 # 2-3", "ACME S.A.S.", "John Doe");

        // Assert
        assertThat(saved.getIdentificationId()).isEqualTo(nit);
        assertThat(saved.getEmail()).isEqualTo(email);
        verify(clientRepositoryPort).save(any(BusinessClient.class));
        verify(auditLogPort).save(eq("BusinessClient"), anyString(),
                eq("REGISTER_BUSINESS_CLIENT"), eq("SYSTEM"),
                any(), eq(null));
    }

    @Test
    void should_throw_BusinessException_when_NIT_already_exists() {
        // Arrange
        when(clientRepositoryPort.existsByIdentificationId("900123456-7")).thenReturn(true);

        RegisterBusinessClientService service = new RegisterBusinessClientService(
                clientRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerBusinessClient("900123456-7",
                "contact@acme.co", "+57 604 1234567", "Cl 1 # 2-3",
                "ACME S.A.S.", "John Doe"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("identification ID");
        verify(clientRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_BusinessException_when_email_already_exists() {
        // Arrange
        when(clientRepositoryPort.existsByIdentificationId("900123456-7")).thenReturn(false);
        when(clientRepositoryPort.existsByEmail("contact@acme.co")).thenReturn(true);

        RegisterBusinessClientService service = new RegisterBusinessClientService(
                clientRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerBusinessClient("900123456-7",
                "contact@acme.co", "+57 604 1234567", "Cl 1 # 2-3",
                "ACME S.A.S.", "John Doe"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");
        verify(clientRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
