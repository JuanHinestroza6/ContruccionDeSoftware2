package co.edu.tdea.bank.domain.model;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Aggregate root that represents a bank account held by a {@link Client}.
 *
 * <p>Invariants enforced at all times:
 * <ul>
 *   <li>Balance never goes below zero.</li>
 *   <li>Deposit and withdrawal amounts must be strictly positive.</li>
 *   <li>No financial operation is allowed when the account is {@code BLOCKED} or {@code CANCELED}.</li>
 * </ul>
 */
public final class BankAccount {

    private final String accountNumber;
    private final AccountType accountType;
    private final Client holder;
    private final CurrencyType currency;
    private final LocalDate openingDate;

    private BigDecimal currentBalance;
    private AccountStatus accountStatus;

    private BankAccount(String accountNumber, AccountType accountType, Client holder,
                        BigDecimal initialBalance, CurrencyType currency,
                        LocalDate openingDate, AccountStatus accountStatus) {
        this.accountNumber  = accountNumber;
        this.accountType    = accountType;
        this.holder         = holder;
        this.currentBalance = initialBalance;
        this.currency       = currency;
        this.openingDate    = openingDate;
        this.accountStatus  = accountStatus;
    }

    /**
     * Factory method — validates all invariants before constructing the account.
     *
     * @param accountNumber  unique account number assigned by the bank
     * @param accountType    savings, checking, or fixed-term
     * @param holder         the {@link Client} who owns this account (never null)
     * @param initialBalance opening balance; must be >= 0
     * @param currency       currency in which the account operates
     */
    public static BankAccount open(String accountNumber,
                                   AccountType accountType,
                                   Client holder,
                                   BigDecimal initialBalance,
                                   CurrencyType currency) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank.");
        }
        Objects.requireNonNull(accountType,    "accountType must not be null.");
        Objects.requireNonNull(holder,         "holder must not be null.");
        Objects.requireNonNull(currency,       "currency must not be null.");
        Objects.requireNonNull(initialBalance, "initialBalance must not be null.");

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "initialBalance must be >= 0, got: " + initialBalance);
        }

        return new BankAccount(
                accountNumber.trim(),
                accountType,
                holder,
                initialBalance,
                currency,
                LocalDate.now(),
                AccountStatus.ACTIVE
        );
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     */
    public static BankAccount reconstruct(String accountNumber,
                                          AccountType accountType,
                                          Client holder,
                                          BigDecimal currentBalance,
                                          CurrencyType currency,
                                          LocalDate openingDate,
                                          AccountStatus accountStatus) {
        Objects.requireNonNull(accountNumber,  "accountNumber must not be null.");
        Objects.requireNonNull(accountType,    "accountType must not be null.");
        Objects.requireNonNull(holder,         "holder must not be null.");
        Objects.requireNonNull(currentBalance, "currentBalance must not be null.");
        Objects.requireNonNull(currency,       "currency must not be null.");
        Objects.requireNonNull(openingDate,    "openingDate must not be null.");
        Objects.requireNonNull(accountStatus,  "accountStatus must not be null.");
        return new BankAccount(accountNumber, accountType, holder,
                               currentBalance, currency, openingDate, accountStatus);
    }

    // --- Status queries ---

    /** Returns {@code true} when the account status is {@code ACTIVE}. */
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    /**
     * Returns {@code true} when financial operations (deposit/withdrawal) are allowed.
     * An account can operate only when it is {@code ACTIVE}.
     */
    public boolean canOperate() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    // --- Financial operations ---

    /**
     * Credits the given amount to this account.
     *
     * @param amount strictly positive amount in the account's currency
     * @throws IllegalStateException     if the account cannot operate
     * @throws IllegalArgumentException  if the amount is null or <= 0
     */
    public void deposit(BigDecimal amount) {
        requireOperable("deposit");
        requirePositiveAmount(amount, "deposit");
        currentBalance = currentBalance.add(amount);
    }

    /**
     * Debits the given amount from this account.
     *
     * @param amount strictly positive amount; must not exceed {@code currentBalance}
     * @throws IllegalStateException     if the account cannot operate or has insufficient funds
     * @throws IllegalArgumentException  if the amount is null or <= 0
     */
    public void withdraw(BigDecimal amount) {
        requireOperable("withdrawal");
        requirePositiveAmount(amount, "withdrawal");

        if (amount.compareTo(currentBalance) > 0) {
            throw new IllegalStateException(
                    "Insufficient balance. Available: " + currentBalance + ", requested: " + amount);
        }
        currentBalance = currentBalance.subtract(amount);
    }

    // --- State transitions ---

    /**
     * Blocks the account. A blocked account cannot perform financial operations
     * but retains its balance and can be unblocked later.
     *
     * @throws IllegalStateException if the account is already CANCELED
     */
    public void block() {
        if (accountStatus == AccountStatus.CANCELED) {
            throw new IllegalStateException("A canceled account cannot be blocked.");
        }
        accountStatus = AccountStatus.BLOCKED;
    }

    /**
     * Permanently cancels the account. Once canceled, no further operations are allowed.
     *
     * @throws IllegalStateException if the account is already CANCELED
     */
    public void cancel() {
        if (accountStatus == AccountStatus.CANCELED) {
            throw new IllegalStateException("Account is already canceled.");
        }
        accountStatus = AccountStatus.CANCELED;
    }

    // --- Getters ---

    public String getAccountNumber()    { return accountNumber; }
    public AccountType getAccountType() { return accountType; }
    public Client getHolder()           { return holder; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public CurrencyType getCurrency()   { return currency; }
    public LocalDate getOpeningDate()   { return openingDate; }
    public AccountStatus getAccountStatus() { return accountStatus; }

    // --- Private guards ---

    private void requireOperable(String operation) {
        if (!canOperate()) {
            throw new IllegalStateException(
                    "Cannot perform " + operation + ": account is " + accountStatus);
        }
    }

    private void requirePositiveAmount(BigDecimal amount, String operation) {
        Objects.requireNonNull(amount, operation + " amount must not be null.");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    operation + " amount must be > 0, got: " + amount);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount that)) return false;
        return Objects.equals(accountNumber, that.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return "BankAccount{number='" + accountNumber + "', type=" + accountType
                + ", holder='" + holder.getClientId() + "', balance=" + currentBalance
                + " " + currency + ", status=" + accountStatus + '}';
    }
}
