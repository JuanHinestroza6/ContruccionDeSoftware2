package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.models.BankAccount;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Input port — opens a new bank account for an existing client.
 * The client is resolved internally from the repository; callers supply only the ID.
 */
public interface OpenBankAccountUseCase {

    /**
     * @param clientId       UUID of the client who will own the account
     * @param accountNumber  unique account number assigned by the bank
     * @param accountType    savings, checking, or fixed-term
     * @param initialBalance opening balance; must be >= 0
     * @param currency       currency in which the account will operate
     * @return the newly created {@link BankAccount} in {@code ACTIVE} status
     */
    BankAccount openAccount(UUID clientId,
                            String accountNumber,
                            AccountType accountType,
                            BigDecimal initialBalance,
                            CurrencyType currency);
}
