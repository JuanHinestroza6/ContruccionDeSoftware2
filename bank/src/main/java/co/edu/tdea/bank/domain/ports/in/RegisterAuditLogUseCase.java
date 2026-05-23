package co.edu.tdea.bank.domain.ports.in;

import java.time.LocalDateTime;

/**
 * Input port — records an immutable audit entry for any significant domain event.
 *
 * <p>Keeps the audit concern decoupled from the operations that trigger it.
 * Each use case that must be audited calls this port after completing its own
 * state change, so audit writes never block or roll back a business operation.
 */
public interface RegisterAuditLogUseCase {

    /**
     * @param entityType    type of the entity affected (e.g. "Loan", "Transfer", "BankAccount")
     * @param entityId      string representation of the entity's identifier
     * @param action        description of the operation performed (e.g. "APPROVED", "DISBURSED")
     * @param performedBy   identifier of the user who triggered the action
     * @param occurredAt    exact moment the action took place
     * @param detail        optional free-text or serialized snapshot of the relevant state;
     *                      may be {@code null} when no additional context is needed
     */
    void registerAuditLog(String entityType,
                          String entityId,
                          String action,
                          String performedBy,
                          LocalDateTime occurredAt,
                          String detail);
}
