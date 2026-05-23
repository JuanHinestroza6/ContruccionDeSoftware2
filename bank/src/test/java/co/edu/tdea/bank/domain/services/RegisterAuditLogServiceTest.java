package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterAuditLogServiceTest {

    @Mock private AuditLogPort auditLogPort;

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 1, 15, 12, 0);

    @Test
    void should_register_audit_log_successfully_when_all_fields_provided() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act
        assertThatCode(() -> service.registerAuditLog("Loan", "42",
                "LOAN_APPROVED", "user-123", OCCURRED_AT,
                "approvedAmount=5000000"))
                .doesNotThrowAnyException();

        // Assert
        verify(auditLogPort).save("Loan", "42", "LOAN_APPROVED",
                "user-123", OCCURRED_AT, "approvedAmount=5000000");
    }

    @Test
    void should_register_audit_log_successfully_when_detail_is_null() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act
        service.registerAuditLog("Transfer", "T-1", "TRANSFER_REJECTED",
                "user-456", OCCURRED_AT, null);

        // Assert — detail is optional and null is allowed
        verify(auditLogPort).save("Transfer", "T-1", "TRANSFER_REJECTED",
                "user-456", OCCURRED_AT, null);
    }

    @Test
    void should_throw_IllegalArgumentException_when_entityType_is_null() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerAuditLog(null, "1", "ACTION",
                "user", OCCURRED_AT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_IllegalArgumentException_when_entityId_is_null() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerAuditLog("Loan", null, "ACTION",
                "user", OCCURRED_AT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityId");
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_IllegalArgumentException_when_action_is_null() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerAuditLog("Loan", "1", null,
                "user", OCCURRED_AT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_IllegalArgumentException_when_performedBy_is_null() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerAuditLog("Loan", "1", "ACTION",
                null, OCCURRED_AT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("performedBy");
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_IllegalArgumentException_when_occurredAt_is_null() {
        // Arrange
        RegisterAuditLogService service = new RegisterAuditLogService(auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.registerAuditLog("Loan", "1", "ACTION",
                "user", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("occurredAt");
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
