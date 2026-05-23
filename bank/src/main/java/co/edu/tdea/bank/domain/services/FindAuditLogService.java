package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.in.FindAuditLogUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service — read-only queries against the immutable audit log.
 *
 * <p>Delegates directly to {@link AuditLogPort}. Empty lists are valid results
 * (no exceptions thrown for absent entries), and audit-log reads are themselves
 * never audited.
 */
@Service
public class FindAuditLogService implements FindAuditLogUseCase {

    private final AuditLogPort auditLogPort;

    public FindAuditLogService(AuditLogPort auditLogPort) {
        this.auditLogPort = auditLogPort;
    }

    @Override
    public List<AuditLogPort.AuditEntry> findByEntity(String entityType, String entityId) {
        return auditLogPort.findByEntity(entityType, entityId);
    }

    @Override
    public List<AuditLogPort.AuditEntry> findByPerformedBy(String performedBy) {
        return auditLogPort.findByPerformedBy(performedBy);
    }
}
