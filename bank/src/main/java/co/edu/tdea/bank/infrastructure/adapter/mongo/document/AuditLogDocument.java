package co.edu.tdea.bank.infrastructure.adapter.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Immutable audit-log document — one record per operation written to the
 * Bitácora de Operaciones in MongoDB.
 *
 * <p>Indexed fields match the most common query paths exposed by
 * {@link co.edu.tdea.bank.application.port.out.AuditLogPort}: {@code findByEntity}
 * (entityType + entityId) and {@code findByPerformedBy} (performedBy), plus
 * {@code occurredAt} for chronological filtering.
 */
@Document(collection = "audit_logs")
public class AuditLogDocument {

    @Id
    private String id;

    /** Type of the affected entity, e.g. "Loan", "Transfer", "BankAccount". */
    @Indexed
    @Field("entity_type")
    private String entityType;

    /** String form of the entity's identifier (UUID, account number, loan id, ...). */
    @Indexed
    @Field("entity_id")
    private String entityId;

    /** Action performed, e.g. "APPROVED", "DISBURSED", "EXECUTED", "EXPIRED". */
    @Field("action")
    private String action;

    /** Identifier of the user that triggered the action (string form). */
    @Indexed
    @Field("performed_by")
    private String performedBy;

    /** Exact moment the action took place. Indexed for chronological queries. */
    @Indexed
    @Field("occurred_at")
    private LocalDateTime occurredAt;

    /** Optional serialized snapshot/detail of the operation. May be {@code null}. */
    @Field("detail")
    private String detail;

    public AuditLogDocument() {}

    public AuditLogDocument(String entityType, String entityId, String action,
                            String performedBy, LocalDateTime occurredAt, String detail) {
        this.entityType  = entityType;
        this.entityId    = entityId;
        this.action      = action;
        this.performedBy = performedBy;
        this.occurredAt  = occurredAt;
        this.detail      = detail;
    }

    public String getId()                  { return id; }
    public String getEntityType()          { return entityType; }
    public String getEntityId()            { return entityId; }
    public String getAction()              { return action; }
    public String getPerformedBy()         { return performedBy; }
    public LocalDateTime getOccurredAt()   { return occurredAt; }
    public String getDetail()              { return detail; }

    public void setId(String id)                       { this.id = id; }
    public void setEntityType(String entityType)       { this.entityType = entityType; }
    public void setEntityId(String entityId)           { this.entityId = entityId; }
    public void setAction(String action)               { this.action = action; }
    public void setPerformedBy(String performedBy)     { this.performedBy = performedBy; }
    public void setOccurredAt(LocalDateTime occurredAt){ this.occurredAt = occurredAt; }
    public void setDetail(String detail)               { this.detail = detail; }
}
