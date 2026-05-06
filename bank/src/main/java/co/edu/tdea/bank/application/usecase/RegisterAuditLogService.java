package co.edu.tdea.bank.application.usecase;

import co.edu.tdea.bank.application.port.in.RegisterAuditLogUseCase;
import co.edu.tdea.bank.application.port.out.AuditLogPort;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

/**
 * Application service — appends an immutable audit entry for any significant domain event.
 *
 * <p>Validates that the mandatory fields are present and delegates persistence to the
 * {@link AuditLogPort}. The {@code detail} field is intentionally optional and may be
 * {@code null} when no additional context is needed.
 */
@Service
public class RegisterAuditLogService implements RegisterAuditLogUseCase {

    private final AuditLogPort auditLogPort;

    public RegisterAuditLogService(AuditLogPort auditLogPort) {
        this.auditLogPort = auditLogPort;
    }

    @Override
    public void registerAuditLog(String entityType,
                                 String entityId,
                                 String action,
                                 String performedBy,
                                 LocalDateTime occurredAt,
                                 String detail) {

        if (entityType == null) {
            throw new IllegalArgumentException("entityType cannot be null");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("entityId cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        if (performedBy == null) {
            throw new IllegalArgumentException("performedBy cannot be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt cannot be null");
        }

        auditLogPort.save(entityType, entityId, action, performedBy, occurredAt, detail);
    }
}
