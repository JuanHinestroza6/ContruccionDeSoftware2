package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.ports.in.FindClientUseCase;
import co.edu.tdea.bank.domain.ports.in.RegisterBusinessClientUseCase;
import co.edu.tdea.bank.domain.ports.in.RegisterIndividualClientUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.client.ClientResponse;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.client.RegisterBusinessClientRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.client.RegisterIndividualClientRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.mapper.ClientDtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST adapter for the {@code Client} aggregate.
 *
 * <p>Pure delegating layer — every endpoint parses input, hands the command
 * off to the corresponding input port, and serializes the domain result via
 * {@link ClientDtoMapper}. No business decisions live in this class.</p>
 *
 * <p>Authorization (S3):
 * <ul>
 *   <li>Registration endpoints are restricted to back-office staff
 *       (tellers, commercial employees, internal analysts).</li>
 *   <li>Read endpoints add ownership filtering via {@code @authz} so a
 *       client may only inspect its own record; staff bypass roles
 *       (analyst / teller / commercial) read any record.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final RegisterIndividualClientUseCase registerIndividualClientUseCase;
    private final RegisterBusinessClientUseCase registerBusinessClientUseCase;
    private final FindClientUseCase findClientUseCase;

    public ClientController(RegisterIndividualClientUseCase registerIndividualClientUseCase,
                            RegisterBusinessClientUseCase registerBusinessClientUseCase,
                            FindClientUseCase findClientUseCase) {
        this.registerIndividualClientUseCase = registerIndividualClientUseCase;
        this.registerBusinessClientUseCase   = registerBusinessClientUseCase;
        this.findClientUseCase               = findClientUseCase;
    }

    /**
     * Registers a new individual (natural person) client.
     */
    @PreAuthorize("hasAnyRole('TELLER_EMPLOYEE','COMMERCIAL_EMPLOYEE','INTERNAL_ANALYST')")
    @PostMapping("/individual")
    public ResponseEntity<ClientResponse> registerIndividualClient(
            @Valid @RequestBody RegisterIndividualClientRequest request) {

        IndividualClient created = registerIndividualClientUseCase.registerIndividualClient(
                request.identificationId(),
                request.email(),
                request.phone(),
                request.address(),
                request.fullName(),
                request.birthDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClientDtoMapper.toResponse(created));
    }

    /**
     * Registers a new business (legal entity) client.
     */
    @PreAuthorize("hasAnyRole('TELLER_EMPLOYEE','COMMERCIAL_EMPLOYEE','INTERNAL_ANALYST')")
    @PostMapping("/business")
    public ResponseEntity<ClientResponse> registerBusinessClient(
            @Valid @RequestBody RegisterBusinessClientRequest request) {

        BusinessClient created = registerBusinessClientUseCase.registerBusinessClient(
                request.identificationId(),
                request.email(),
                request.phone(),
                request.address(),
                request.companyName(),
                request.legalRepresentative()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClientDtoMapper.toResponse(created));
    }

    /**
     * Retrieves a client by its technical identifier.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST') or @authz.hasAccessToClient(authentication, #clientId)")
    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> findClientById(@PathVariable UUID clientId) {
        Client client = findClientUseCase.findById(clientId);
        return ResponseEntity.ok(ClientDtoMapper.toResponse(client));
    }

    /**
     * Retrieves a client by its national identification number (Cédula / NIT).
     */
    @PreAuthorize("hasAnyRole('INTERNAL_ANALYST','TELLER_EMPLOYEE','COMMERCIAL_EMPLOYEE')")
    @GetMapping("/by-identification/{identificationId}")
    public ResponseEntity<ClientResponse> findClientByIdentificationId(
            @PathVariable String identificationId) {

        Client client = findClientUseCase.findByIdentificationId(identificationId);
        return ResponseEntity.ok(ClientDtoMapper.toResponse(client));
    }
}
