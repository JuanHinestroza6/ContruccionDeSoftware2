package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.ports.out.AuditLogPort;

import java.util.List;

/**
 * Input port — read-only queries against the immutable audit log.
 *
 * <p>Returns flat {@link AuditLogPort.AuditEntry} projections defined by the
 * output port, keeping the contract self-contained without a separate DTO.
 * Queries against the audit log do not themselves generate audit entries.
 */
public interface FindAuditLogUseCase {

    /**
     * Returns audit entries for the given entity type and identifier,
     * ordered by {@code occurredAt} ascending.
     *
     * @param entityType type of the entity (e.g. "Loan", "Transfer", "BankAccount")
     * @param entityId   identifier of the entity instance
     * @return matching entries, or an empty list if none exist
     */
    List<AuditLogPort.AuditEntry> findByEntity(String entityType, String entityId);

    /**
     * Returns all audit entries recorded by the given user,
     * ordered by {@code occurredAt} ascending.
     *
     * @param performedBy identifier of the user whose actions are being queried
     * @return matching entries, or an empty list if none exist
     */
    List<AuditLogPort.AuditEntry> findByPerformedBy(String performedBy);
}
