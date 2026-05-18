package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.BusinessException;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.in.RegisterIndividualClientUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Application service — registers a new {@link IndividualClient}.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>Identification number must be unique across all clients.</li>
 *   <li>Email must be unique across all clients.</li>
 *   <li>Domain invariants (age >= 18, blank checks, etc.) are delegated to
 *       {@link IndividualClient#create}.</li>
 *   <li>The persisted instance is appended to the immutable audit log.</li>
 * </ol>
 */
@Service
public class RegisterIndividualClientService implements RegisterIndividualClientUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final AuditLogPort auditLogPort;

    public RegisterIndividualClientService(ClientRepositoryPort clientRepositoryPort,
                                           AuditLogPort auditLogPort) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.auditLogPort         = auditLogPort;
    }

    @Override
    public IndividualClient registerIndividualClient(String identificationId,
                                                     String email,
                                                     String phone,
                                                     String address,
                                                     String fullName,
                                                     LocalDate birthDate) {

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
        IndividualClient client = IndividualClient.create(
                identificationId,
                email,
                phone,
                address,
                fullName,
                birthDate
        );

        // 4. Persist
        IndividualClient saved = (IndividualClient) clientRepositoryPort.save(client);

        // 5. Audit
        auditLogPort.save(
                "IndividualClient",
                saved.getClientId().toString(),
                "REGISTER_INDIVIDUAL_CLIENT",
                "SYSTEM",
                LocalDateTime.now(),
                null
        );

        return saved;
    }
}
