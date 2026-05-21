package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort.AuditEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAuditLogServiceTest {

    @Mock private AuditLogPort auditLogPort;

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 1, 15, 12, 0);

    @Test
    void should_return_entries_when_queried_by_entity() {
        // Arrange
        AuditEntry entry = new AuditEntry("Loan", "42", "LOAN_APPROVED",
                "user-1", OCCURRED_AT, "detail");
        when(auditLogPort.findByEntity("Loan", "42")).thenReturn(List.of(entry));

        FindAuditLogService service = new FindAuditLogService(auditLogPort);

        // Act
        List<AuditEntry> result = service.findByEntity("Loan", "42");

        // Assert
        assertThat(result).containsExactly(entry);
    }

    @Test
    void should_return_entries_when_queried_by_performed_by() {
        // Arrange
        AuditEntry entry = new AuditEntry("Transfer", "T-1", "TRANSFER_APPROVED",
                "user-1", OCCURRED_AT, null);
        when(auditLogPort.findByPerformedBy("user-1")).thenReturn(List.of(entry));

        FindAuditLogService service = new FindAuditLogService(auditLogPort);

        // Act
        List<AuditEntry> result = service.findByPerformedBy("user-1");

        // Assert
        assertThat(result).containsExactly(entry);
    }

    @Test
    void should_return_empty_list_when_no_entries_for_entity() {
        // Arrange — empty result is a valid outcome (no exception thrown)
        when(auditLogPort.findByEntity("Loan", "9999")).thenReturn(List.of());

        FindAuditLogService service = new FindAuditLogService(auditLogPort);

        // Act
        List<AuditEntry> result = service.findByEntity("Loan", "9999");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_list_when_no_entries_for_performed_by() {
        // Arrange
        when(auditLogPort.findByPerformedBy("ghost-user")).thenReturn(List.of());

        FindAuditLogService service = new FindAuditLogService(auditLogPort);

        // Act
        List<AuditEntry> result = service.findByPerformedBy("ghost-user");

        // Assert
        assertThat(result).isEmpty();
    }
}
