package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.ports.in.FindAuditLogUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.audit.AuditLogResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST adapter for read-only queries against the immutable audit log.
 *
 * <p>Pure delegating layer — every endpoint forwards the call to
 * {@link FindAuditLogUseCase} and converts the flat
 * {@link AuditLogPort.AuditEntry} projections returned by the port into
 * {@link AuditLogResponse} instances. No business decisions live in this
 * class. Audit log queries do not themselves generate audit entries.</p>
 *
 * <p>Authorization (S3):
 * <ul>
 *   <li>Both endpoints are reserved for {@code INTERNAL_ANALYST}; per-client
 *       self-service over the audit log will land in S4 with a dedicated
 *       {@code by-self} endpoint.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoría", description = "Consulta del log de auditoría MongoDB (solo INTERNAL_ANALYST)")
public class AuditLogController {

    private final FindAuditLogUseCase findAuditLogUseCase;

    public AuditLogController(FindAuditLogUseCase findAuditLogUseCase) {
        this.findAuditLogUseCase = findAuditLogUseCase;
    }

    /**
     * Returns every audit entry recorded for the given entity type and
     * identifier, ordered by {@code occurredAt} ascending. Returns an empty
     * list (never 404) when the entity has no entries.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST')")
    @GetMapping("/by-entity")
    public ResponseEntity<List<AuditLogResponse>> findByEntity(
            @RequestParam String entityType,
            @RequestParam String entityId) {

        List<AuditLogResponse> entries = findAuditLogUseCase
                .findByEntity(entityType, entityId)
                .stream()
                .map(e -> new AuditLogResponse(
                        e.entityType(),
                        e.entityId(),
                        e.action(),
                        e.performedBy(),
                        e.occurredAt(),
                        e.detail()))
                .toList();
        return ResponseEntity.ok(entries);
    }

    /**
     * Returns every audit entry recorded by the given user, ordered by
     * {@code occurredAt} ascending. Returns an empty list (never 404) when
     * the user has no entries.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST')")
    @GetMapping("/by-user")
    public ResponseEntity<List<AuditLogResponse>> findByPerformedBy(
            @RequestParam String performedBy) {

        List<AuditLogResponse> entries = findAuditLogUseCase
                .findByPerformedBy(performedBy)
                .stream()
                .map(e -> new AuditLogResponse(
                        e.entityType(),
                        e.entityId(),
                        e.action(),
                        e.performedBy(),
                        e.occurredAt(),
                        e.detail()))
                .toList();
        return ResponseEntity.ok(entries);
    }
}
