package co.edu.tdea.bank.infrastructure.adapter.mongo;

import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.infrastructure.adapter.mongo.mapper.AuditLogMapper;
import co.edu.tdea.bank.infrastructure.adapter.mongo.repository.AuditLogMongoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB adapter for {@link AuditLogPort}. Writes append-only audit entries
 * into the {@code audit_logs} collection. Reads return the immutable
 * {@link AuditEntry} read projection â€” never the raw document.
 */
@Component
public class AuditLogAdapter implements AuditLogPort {

    private final AuditLogMongoRepository mongo;

    public AuditLogAdapter(AuditLogMongoRepository mongo) {
        this.mongo = mongo;
    }

    @Override
    public void save(String entityType,
                     String entityId,
                     String action,
                     String performedBy,
                     LocalDateTime occurredAt,
                     String detail) {
        mongo.save(AuditLogMapper.toDocument(entityType, entityId, action,
                                             performedBy, occurredAt, detail));
    }

    @Override
    public List<AuditEntry> findByEntity(String entityType, String entityId) {
        return AuditLogMapper.toEntryList(
                mongo.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(entityType, entityId));
    }

    @Override
    public List<AuditEntry> findByPerformedBy(String performedBy) {
        return AuditLogMapper.toEntryList(
                mongo.findByPerformedByOrderByOccurredAtAsc(performedBy));
    }
}
