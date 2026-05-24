package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.ports.in.FindBankAccountUseCase;
import co.edu.tdea.bank.domain.ports.in.OpenBankAccountUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.account.BankAccountResponse;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.account.OpenBankAccountRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.mapper.BankAccountDtoMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the {@code BankAccount} aggregate.
 *
 * <p>Pure delegating layer — every endpoint validates input, hands the call
 * off to the corresponding input port, and serializes the domain result via
 * {@link BankAccountDtoMapper}. No business decisions live in this class.</p>
 *
 * <p>Authorization (S3):
 * <ul>
 *   <li>Account opening is restricted to teller / commercial employees.</li>
 *   <li>Reads add ownership filtering via {@code @authz} so a client may only
 *       look at its own account; analysts bypass ownership.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Cuentas Bancarias", description = "Apertura y consulta de cuentas bancarias")
public class BankAccountController {

    private final OpenBankAccountUseCase openBankAccountUseCase;
    private final FindBankAccountUseCase findBankAccountUseCase;

    public BankAccountController(OpenBankAccountUseCase openBankAccountUseCase,
                                 FindBankAccountUseCase findBankAccountUseCase) {
        this.openBankAccountUseCase = openBankAccountUseCase;
        this.findBankAccountUseCase = findBankAccountUseCase;
    }

    /**
     * Opens a new bank account for an existing client.
     */
    @PreAuthorize("hasAnyRole('TELLER_EMPLOYEE','COMMERCIAL_EMPLOYEE')")
    @PostMapping
    public ResponseEntity<BankAccountResponse> openAccount(
            @Valid @RequestBody OpenBankAccountRequest request) {

        BankAccount created = openBankAccountUseCase.openAccount(
                request.clientId(),
                request.accountNumber(),
                request.accountType(),
                request.initialBalance(),
                request.currency()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BankAccountDtoMapper.toResponse(created));
    }

    /**
     * Retrieves a bank account by its natural business key.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST') or @authz.hasAccessToAccount(authentication, #accountNumber)")
    @GetMapping("/{accountNumber}")
    public ResponseEntity<BankAccountResponse> findByAccountNumber(
            @PathVariable String accountNumber) {

        BankAccount account = findBankAccountUseCase.findByAccountNumber(accountNumber);
        return ResponseEntity.ok(BankAccountDtoMapper.toResponse(account));
    }

    /**
     * Lists every bank account held by the given client. Returns an empty
     * list (never 404) when the client has no accounts.
     */
    @PreAuthorize("hasRole('INTERNAL_ANALYST') or @authz.hasAccessToClient(authentication, #clientId)")
    @GetMapping("/by-client/{clientId}")
    public ResponseEntity<List<BankAccountResponse>> findByClientId(
            @PathVariable UUID clientId) {

        List<BankAccountResponse> accounts = findBankAccountUseCase.findByClientId(clientId)
                .stream()
                .map(BankAccountDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(accounts);
    }
}
