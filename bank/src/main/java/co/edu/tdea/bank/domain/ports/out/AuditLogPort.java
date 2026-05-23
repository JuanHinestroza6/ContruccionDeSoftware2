package co.edu.tdea.bank.domain.ports.out;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Output port — contract for writing and querying the immutable audit log.
 *
 * <p>Implementations may back this with a NoSQL store (document per entry)
 * or any append-only persistence mechanism. Entries must never be mutated
 * or deleted after being written.
 */
public interface AuditLogPort {

    /**
     * Appends an immutable audit entry.
     *
     * @param entityType  type of the entity affected (e.g. "Loan", "Transfer", "BankAccount")
     * @param entityId    string representation of the entity's identifier
     * @param action      description of the operation performed (e.g. "APPROVED", "DISBURSED")
     * @param performedBy identifier of the user who triggered the action
     * @param occurredAt  exact moment the action took place
     * @param detail      optional serialized snapshot of the relevant state; may be {@code null}
     */
    void save(String entityType,
              String entityId,
              String action,
              String performedBy,
              LocalDateTime occurredAt,
              String detail);

    /**
     * Returns all audit entries for a specific entity, ordered by {@code occurredAt} ascending.
     *
     * @param entityType type of the entity (e.g. "Loan")
     * @param entityId   identifier of the entity instance
     */
    List<AuditEntry> findByEntity(String entityType, String entityId);

    /**
     * Returns all audit entries recorded by a specific user, ordered by {@code occurredAt} ascending.
     *
     * @param performedBy identifier of the user whose actions are being queried
     */
    List<AuditEntry> findByPerformedBy(String performedBy);

    // -------------------------------------------------------------------------
    // Read model — flat projection of an audit record, decoupled from NoSQL schema
    // -------------------------------------------------------------------------

    /**
     * Immutable read projection of a single audit log entry.
     * Defined here as a record to keep the port self-contained without requiring a separate DTO.
     */
    record AuditEntry(String entityType,
                      String entityId,
                      String action,
                      String performedBy,
                      LocalDateTime occurredAt,
                      String detail) {}
}
