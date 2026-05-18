package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.BankAccount;

import java.util.List;
import java.util.UUID;

/**
 * Input port — query operations for the {@link BankAccount} aggregate.
 */
public interface FindBankAccountUseCase {

    /**
     * Finds a bank account by its natural business key.
     * Throws {@code ResourceNotFoundException} if no account exists with the given number.
     *
     * @param accountNumber natural business key of the account
     * @return the bank account
     */
    BankAccount findByAccountNumber(String accountNumber);

    /**
     * Returns all bank accounts belonging to the given client.
     * Returns an empty list if the client has no accounts (does not throw).
     *
     * @param clientId technical UUID of the client
     * @return list of accounts (possibly empty)
     */
    List<BankAccount> findByClientId(UUID clientId);
}
