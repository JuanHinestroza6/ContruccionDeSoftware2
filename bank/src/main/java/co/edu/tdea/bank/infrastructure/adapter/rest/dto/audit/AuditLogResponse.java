package co.edu.tdea.bank.infrastructure.adapter.rest.dto.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Outbound representation of a single audit log entry.
 *
 * <p>Mirrors {@code AuditLogPort.AuditEntry} one-to-one so the wire format
 * stays decoupled from the persistence-side projection while remaining a
 * faithful read model of the immutable log.</p>
 *
 * <p>{@code @JsonInclude(NON_NULL)} omits {@code detail} when no serialized
 * snapshot was attached to the entry.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogResponse(

        String entityType,
        String entityId,
        String action,
        String performedBy,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime occurredAt,

        String detail
) {
}
