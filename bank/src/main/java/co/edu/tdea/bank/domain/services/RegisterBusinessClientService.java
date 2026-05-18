package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.BusinessException;
import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.ports.in.RegisterBusinessClientUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Application service — registers a new {@link BusinessClient}.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>Identification number (NIT) must be unique across all clients.</li>
 *   <li>Email must be unique across all clients.</li>
 *   <li>Domain invariants (company name, legal representative, etc.) are
 *       delegated to {@link BusinessClient#create}.</li>
 *   <li>The persisted instance is appended to the immutable audit log.</li>
 * </ol>
 */
@Service
public class RegisterBusinessClientService implements RegisterBusinessClientUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AuditLogPort auditLogPort;

    public RegisterBusinessClientService(ClientRepositoryPort clientRepositoryPort,
                                         AuditLogPort auditLogPort) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.auditLogPort         = auditLogPort;
    }

    @Override
    public BusinessClient registerBusinessClient(String identificationId,
                                                 String email,
                                                 String phone,
                                                 String address,
                                                 String companyName,
                                                 String legalRepresentative) {

        // 1. Identification must be unique
        if (clientRepositoryPort.existsByIdentificationId(identificationId)) {
            throw new BusinessException(
                    "A client with identification ID '" + identificationId + "' already exists");
        }

        // 2. Email must be unique
        if (clientRepositoryPort.existsByEmail(email)) {
            throw new BusinessException(
                    "A client with email '" + email + "' already exists");
        }

        // 3. Build aggregate — domain factory enforces all invariants
        BusinessClient client = BusinessClient.create(
                identificationId,
                email,
                phone,
                address,
                companyName,
                legalRepresentative
        );

        // 4. Persist
        BusinessClient saved = (BusinessClient) clientRepositoryPort.save(client);

        // 5. Audit
        auditLogPort.save(
                "BusinessClient",
                saved.getClientId().toString(),
                "REGISTER_BUSINESS_CLIENT",
                "SYSTEM",
                LocalDateTime.now(),
                null
        );

        return saved;
    }
}
