package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.ports.in.FindAuditLogUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort.AuditEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc slice tests for {@link AuditLogController}.
 *
 * <p>The controller is a thin read-only adapter: it forwards every call to
 * {@link FindAuditLogUseCase} and maps {@code AuditEntry} projections one-to-one
 * to {@code AuditLogResponse}. Verifies HTTP wiring, query-parameter binding
 * and pure delegation to the input port.
 */
@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindAuditLogUseCase findAuditLogUseCase;

    @Test
    void should_return_200_with_entries_when_found_by_entity() throws Exception {
        // Arrange
        String entityType = "Loan";
        String entityId = "42";
        AuditEntry e1 = new AuditEntry(
                entityType,
                entityId,
                "REQUESTED",
                "user-1",
                LocalDateTime.of(2026, 1, 1, 10, 0, 0),
                "{\"requestedAmount\":5000000}");
        AuditEntry e2 = new AuditEntry(
                entityType,
                entityId,
                "APPROVED",
                "analyst-1",
                LocalDateTime.of(2026, 1, 1, 11, 0, 0),
                null);
        when(findAuditLogUseCase.findByEntity(entityType, entityId))
                .thenReturn(List.of(e1, e2));

        // Act + Assert
        mockMvc.perform(get("/api/v1/audit/by-entity")
                        .param("entityType", entityType)
                        .param("entityId", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].entityType").value(entityType))
                .andExpect(jsonPath("$[0].entityId").value(entityId))
                .andExpect(jsonPath("$[0].action").value("REQUESTED"))
                .andExpect(jsonPath("$[0].performedBy").value("user-1"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-01-01T10:00:00"))
                .andExpect(jsonPath("$[0].detail").value("{\"requestedAmount\":5000000}"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"))
                .andExpect(jsonPath("$[1].performedBy").value("analyst-1"))
                // detail is null → omitted by @JsonInclude(NON_NULL)
                .andExpect(jsonPath("$[1].detail").doesNotExist());

        verify(findAuditLogUseCase).findByEntity(entityType, entityId);
    }

    @Test
    void should_return_200_with_empty_list_when_no_entries_found() throws Exception {
        // Arrange
        String entityType = "Loan";
        String entityId = "999";
        when(findAuditLogUseCase.findByEntity(entityType, entityId))
                .thenReturn(Collections.emptyList());

        // Act + Assert
        mockMvc.perform(get("/api/v1/audit/by-entity")
                        .param("entityType", entityType)
                        .param("entityId", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(findAuditLogUseCase).findByEntity(entityType, entityId);
    }

    @Test
    void should_return_200_with_entries_when_found_by_user() throws Exception {
        // Arrange
        String performedBy = "analyst-1";
        AuditEntry e1 = new AuditEntry(
                "Loan",
                "42",
                "APPROVED",
                performedBy,
                LocalDateTime.of(2026, 1, 1, 11, 0, 0),
                null);
        AuditEntry e2 = new AuditEntry(
                "Transfer",
                "100",
                "APPROVED",
                performedBy,
                LocalDateTime.of(2026, 1, 2, 9, 30, 0),
                "{\"amount\":250000}");
        when(findAuditLogUseCase.findByPerformedBy(performedBy))
                .thenReturn(List.of(e1, e2));

        // Act + Assert
        mockMvc.perform(get("/api/v1/audit/by-user")
                        .param("performedBy", performedBy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].entityType").value("Loan"))
                .andExpect(jsonPath("$[0].action").value("APPROVED"))
                .andExpect(jsonPath("$[0].performedBy").value(performedBy))
                .andExpect(jsonPath("$[1].entityType").value("Transfer"))
                .andExpect(jsonPath("$[1].entityId").value("100"))
                .andExpect(jsonPath("$[1].performedBy").value(performedBy));

        verify(findAuditLogUseCase).findByPerformedBy(performedBy);
    }
}
