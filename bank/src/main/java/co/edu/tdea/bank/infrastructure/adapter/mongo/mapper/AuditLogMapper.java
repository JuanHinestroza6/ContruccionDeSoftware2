package co.edu.tdea.bank.infrastructure.adapter.mongo.mapper;

import co.edu.tdea.bank.domain.ports.out.AuditLogPort.AuditEntry;
import co.edu.tdea.bank.infrastructure.adapter.mongo.document.AuditLogDocument;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility â€” converts between {@link AuditLogDocument} and the
 * read-projection {@link AuditEntry} record exposed by {@code AuditLogPort}.
 */
public final class AuditLogMapper {

    private AuditLogMapper() {}

    public static AuditLogDocument toDocument(String entityType,
                                              String entityId,
                                              String action,
                                              String performedBy,
                                              java.time.LocalDateTime occurredAt,
                                              String detail) {
        return new AuditLogDocument(entityType, entityId, action, performedBy, occurredAt, detail);
    }

    public static AuditEntry toEntry(AuditLogDocument doc) {
        if (doc == null) return null;
        return new AuditEntry(
                doc.getEntityType(),
                doc.getEntityId(),
                doc.getAction(),
                doc.getPerformedBy(),
                doc.getOccurredAt(),
                doc.getDetail()
        );
    }

    public static List<AuditEntry> toEntryList(List<AuditLogDocument> docs) {
        if (docs == null) return List.of();
        return docs.stream()
                .map(AuditLogMapper::toEntry)
                .collect(Collectors.toList());
    }
}
