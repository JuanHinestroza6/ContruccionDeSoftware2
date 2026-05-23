package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.in.OpenBankAccountUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;
import co.edu.tdea.bank.domain.Exceptions.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service â€” orchestrates the opening of a new bank account.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The client must exist in the system.</li>
 *   <li>A system user linked to that client must exist.</li>
 *   <li>The linked user must not be {@code INACTIVE} or {@code BLOCKED}.</li>
 *   <li>The requested account number must not already be registered.</li>
 *   <li>The account is created, persisted, and the operation is audited.</li>
 * </ol>
 */
@Service
public class OpenBankAccountService implements OpenBankAccountUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final BankAccountRepositoryPort bankAccountRepositoryPort;
    private final AuditLogPort auditLogPort;

    public OpenBankAccountService(ClientRepositoryPort clientRepositoryPort,
                                  UserRepositoryPort userRepositoryPort,
                                  BankAccountRepositoryPort bankAccountRepositoryPort,
                                  AuditLogPort auditLogPort) {
        this.clientRepositoryPort     = clientRepositoryPort;
        this.userRepositoryPort       = userRepositoryPort;
        this.bankAccountRepositoryPort = bankAccountRepositoryPort;
        this.auditLogPort             = auditLogPort;
    }

    @Override
    public BankAccount openAccount(UUID clientId,
                                   String accountNumber,
                                   AccountType accountType,
                                   BigDecimal initialBalance,
                                   CurrencyType currency) {

        // 1. Client must exist
        Client client = clientRepositoryPort.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        // 2. A system user linked to this client must exist
        User relatedUser = userRepositoryPort.findByRelatedClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System user linked to client", clientId));

        // 3. User must not be INACTIVE
        if (relatedUser.getUserStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedOperationException(
                    "Cannot open account: linked user " + relatedUser.getUserId()
                    + " is INACTIVE.");
        }

        // 4. User must not be BLOCKED
        if (relatedUser.getUserStatus() == UserStatus.BLOCKED) {
            throw new UnauthorizedOperationException(
                    "Cannot open account: linked user " + relatedUser.getUserId()
                    + " is BLOCKED.");
        }

        // 5. Account number must be unique
        if (bankAccountRepositoryPort.existsByAccountNumber(accountNumber)) {
            throw new BusinessException(
                    "Account number already exists: " + accountNumber);
        }

        // 6. Create the account via the domain factory method
        BankAccount account = BankAccount.open(accountNumber, accountType, client,
                                               initialBalance, currency);

        // 7. Persist
        BankAccount savedAccount = bankAccountRepositoryPort.save(account);

        // 8. Audit
        auditLogPort.save(
                "BankAccount",
                savedAccount.getAccountNumber(),
                "ACCOUNT_OPENED",
                relatedUser.getUserId().toString(),
                LocalDateTime.now(),
                "clientId=" + clientId
                + ", type=" + accountType
                + ", currency=" + currency
                + ", initialBalance=" + initialBalance
        );

        return savedAccount;
    }
}
