package co.edu.tdea.bank.domain.ports.out;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.models.BankAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — persistence contract for {@link BankAccount} aggregate.
 */
public interface BankAccountRepositoryPort {

    /** Persists a new account or updates an existing one. Returns the saved instance. */
    BankAccount save(BankAccount account);

    /** Finds an account by its natural business key. */
    Optional<BankAccount> findByAccountNumber(String accountNumber);

    /** Returns all accounts belonging to the given client. */
    List<BankAccount> findByClientId(UUID clientId);

    /** Returns all accounts of the given type belonging to the given client. */
    List<BankAccount> findByClientIdAndType(UUID clientId, AccountType accountType);

    /** Returns all accounts currently in the given status. */
    List<BankAccount> findByStatus(AccountStatus status);

    /** Returns true if an account with the given number already exists. */
    boolean existsByAccountNumber(String accountNumber);
}
