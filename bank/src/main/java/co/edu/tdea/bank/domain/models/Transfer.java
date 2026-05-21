package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root that represents a fund transfer between two bank accounts.
 *
 * <p>State machine:
 * <pre>
 *   PENDING_APPROVAL ──► EXECUTED
 *          │
 *          ├──────────── REJECTED
 *          │
 *          └──────────── EXPIRED
 * </pre>
 *
 * <p>Invariants enforced at all times:
 * <ul>
 *   <li>{@code amount} is strictly positive.</li>
 *   <li>{@code sourceAccount} and {@code destinationAccount} are never null.</li>
 *   <li>{@code createdBy} is never null.</li>
 *   <li>Execution requires both accounts to be operable and source to have sufficient balance.</li>
 *   <li>{@code approve()}, {@code reject()} and {@code expire()} only act on {@code PENDING_APPROVAL}.</li>
 *   <li>{@code execute()} is protected against double-execution by checking terminal states.</li>
 * </ul>
 */
public final class Transfer {

    private final Long transferId;
    private final BankAccount sourceAccount;
    private final BankAccount destinationAccount;
    private final BigDecimal amount;
    private final LocalDateTime creationDateTime;
    private final User createdBy;

    private LocalDateTime approvalDateTime;
    private TransferStatus transferStatus;
    private User approvedBy;

    private Transfer(Long transferId,
                     BankAccount sourceAccount,
                     BankAccount destinationAccount,
                     BigDecimal amount,
                     LocalDateTime creationDateTime,
                     User createdBy) {
        this.transferId          = transferId;
        this.sourceAccount       = sourceAccount;
        this.destinationAccount  = destinationAccount;
        this.amount              = amount;
        this.creationDateTime    = creationDateTime;
        this.createdBy           = createdBy;
        this.transferStatus      = TransferStatus.PENDING_APPROVAL;
    }

    /**
     * Factory method — validates all invariants and creates a transfer in {@code PENDING_APPROVAL}.
     *
     * @param transferId         unique identifier for this transfer
     * @param sourceAccount      account to be debited; must not be null
     * @param destinationAccount account to be credited; must not be null
     * @param amount             amount to transfer; must be > 0
     * @param creationDateTime   moment the transfer was initiated; must not be null
     * @param createdBy          user who initiated the transfer; must not be null
     */
    public static Transfer create(BankAccount sourceAccount,
                                  BankAccount destinationAccount,
                                  BigDecimal amount,
                                  LocalDateTime creationDateTime,
                                  User createdBy) {
        Objects.requireNonNull(sourceAccount,      "sourceAccount must not be null.");
        Objects.requireNonNull(destinationAccount, "destinationAccount must not be null.");
        Objects.requireNonNull(amount,             "amount must not be null.");
        Objects.requireNonNull(creationDateTime,   "creationDateTime must not be null.");
        Objects.requireNonNull(createdBy,          "createdBy must not be null.");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0, got: " + amount);
        }
        if (sourceAccount.equals(destinationAccount)) {
            throw new IllegalArgumentException(
                    "sourceAccount and destinationAccount must be different accounts.");
        }

        return new Transfer(null, sourceAccount, destinationAccount,
                            amount, creationDateTime, createdBy);
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     *
     * <p>Validaciones mínimas de coherencia de estado:
     * <ul>
     *   <li>{@code EXECUTED} ⇒ {@code approvalDateTime} y {@code approvedBy} no nulos.</li>
     *   <li>{@code REJECTED} ⇒ {@code approvedBy} no nulo (quien rechazó).</li>
     * </ul>
     */
    public static Transfer reconstruct(Long transferId,
                                       BankAccount sourceAccount,
                                       BankAccount destinationAccount,
                                       BigDecimal amount,
                                       LocalDateTime creationDateTime,
                                       User createdBy,
                                       LocalDateTime approvalDateTime,
                                       TransferStatus transferStatus,
                                       User approvedBy) {
        Objects.requireNonNull(transferId,         "transferId must not be null.");
        Objects.requireNonNull(sourceAccount,      "sourceAccount must not be null.");
        Objects.requireNonNull(destinationAccount, "destinationAccount must not be null.");
        Objects.requireNonNull(amount,             "amount must not be null.");
        Objects.requireNonNull(creationDateTime,   "creationDateTime must not be null.");
        Objects.requireNonNull(createdBy,          "createdBy must not be null.");
        Objects.requireNonNull(transferStatus,     "transferStatus must not be null.");

        if (transferStatus == TransferStatus.EXECUTED) {
            Objects.requireNonNull(approvalDateTime, "approvalDateTime must not be null when status is EXECUTED.");
            Objects.requireNonNull(approvedBy,       "approvedBy must not be null when status is EXECUTED.");
        }
        if (transferStatus == TransferStatus.REJECTED) {
            Objects.requireNonNull(approvedBy, "approvedBy must not be null when status is REJECTED.");
        }

        Transfer transfer = new Transfer(transferId, sourceAccount, destinationAccount,
                                         amount, creationDateTime, createdBy);
        transfer.approvalDateTime = approvalDateTime;
        transfer.transferStatus   = transferStatus;
        transfer.approvedBy       = approvedBy;
        return transfer;
    }

    // -------------------------------------------------------------------------
    // Business query
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the transfer amount meets or exceeds the approval threshold,
     * meaning an explicit {@link #approve(User, LocalDateTime)} call is required before funds move.
     *
     * @param threshold minimum amount from which approval is mandatory; must be > 0
     */
    public boolean requiresApproval(BigDecimal threshold) {
        Objects.requireNonNull(threshold, "threshold must not be null.");
        if (threshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("threshold must be > 0, got: " + threshold);
        }
        return amount.compareTo(threshold) > 0;
    }

    /**
     * Returns {@code true} when the transfer is still in {@code PENDING_APPROVAL}
     * and the elapsed time since creation exceeds the given expiration window.
     *
     * @param now              current date-time reference
     * @param expirationWindow maximum allowed wait time before the transfer expires
     */
    public boolean isExpired(LocalDateTime now, Duration expirationWindow) {
        Objects.requireNonNull(now,              "now must not be null.");
        Objects.requireNonNull(expirationWindow, "expirationWindow must not be null.");
        if (transferStatus != TransferStatus.PENDING_APPROVAL) {
            return false;
        }
        return Duration.between(creationDateTime, now).compareTo(expirationWindow) > 0;
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    /**
     * Executes the transfer by debiting the source account and crediting the destination account.
     *
     * <p>Preconditions:
     * <ul>
     *   <li>Transfer must not be in a terminal state ({@code EXECUTED}, {@code REJECTED},
     *       {@code EXPIRED}).</li>
     *   <li>Both accounts must be operable.</li>
     *   <li>Source account must have sufficient balance.</li>
     * </ul>
     *
     * @throws IllegalStateException if any precondition is violated
     */
    public void execute() {
        if (transferStatus == TransferStatus.EXECUTED
                || transferStatus == TransferStatus.REJECTED
                || transferStatus == TransferStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Cannot execute: transfer is already " + transferStatus + ".");
        }
        if (!sourceAccount.canOperate()) {
            throw new IllegalStateException(
                    "Cannot execute: source account is " + sourceAccount.getAccountStatus() + ".");
        }
        if (!destinationAccount.canOperate()) {
            throw new IllegalStateException(
                    "Cannot execute: destination account is "
                    + destinationAccount.getAccountStatus() + ".");
        }

        // withdraw enforces sufficient-balance invariant — no duplicate check needed here
        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        this.transferStatus = TransferStatus.EXECUTED;
    }

    /**
     * Approves the transfer and immediately executes it.
     *
     * @param approver         user granting the approval; must not be null
     * @param approvalDateTime moment of approval; must not be null
     * @throws IllegalStateException if the transfer is not in {@code PENDING_APPROVAL}
     */
    public void approve(User approver, LocalDateTime approvalDateTime) {
        requirePendingApproval("approve");
        Objects.requireNonNull(approver,         "approver must not be null.");
        Objects.requireNonNull(approvalDateTime, "approvalDateTime must not be null.");

        this.approvedBy        = approver;
        this.approvalDateTime  = approvalDateTime;

        execute();
    }

    /**
     * Rejects the transfer. The transfer moves to a terminal state; no funds are moved.
     *
     * @param approver user rejecting the transfer; must not be null
     * @throws IllegalStateException if the transfer is not in {@code PENDING_APPROVAL}
     */
    public void reject(User approver) {
        requirePendingApproval("reject");
        Objects.requireNonNull(approver, "approver must not be null.");

        this.approvedBy    = approver;
        this.transferStatus = TransferStatus.REJECTED;
    }

    /**
     * Marks the transfer as expired. Called by a scheduled job when
     * {@link #isExpired(LocalDateTime, Duration)} returns {@code true}.
     *
     * @throws IllegalStateException if the transfer is not in {@code PENDING_APPROVAL}
     */
    public void expire() {
        requirePendingApproval("expire");
        this.transferStatus = TransferStatus.EXPIRED;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getTransferId()                  { return transferId; }
    public BankAccount getSourceAccount()        { return sourceAccount; }
    public BankAccount getDestinationAccount()   { return destinationAccount; }
    public BigDecimal getAmount()                { return amount; }
    public LocalDateTime getCreationDateTime()   { return creationDateTime; }
    public LocalDateTime getApprovalDateTime()   { return approvalDateTime; }
    public TransferStatus getTransferStatus()    { return transferStatus; }
    public User getCreatedBy()                   { return createdBy; }
    public User getApprovedBy()                  { return approvedBy; }

    // -------------------------------------------------------------------------
    // Private guard
    // -------------------------------------------------------------------------

    private void requirePendingApproval(String operation) {
        if (transferStatus != TransferStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Cannot " + operation + ": transfer is " + transferStatus
                    + ", expected PENDING_APPROVAL.");
        }
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transfer transfer)) return false;
        return Objects.equals(transferId, transfer.transferId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transferId);
    }

    @Override
    public String toString() {
        return "Transfer{transferId=" + transferId
                + ", amount=" + amount
                + ", status=" + transferStatus
                + ", from='" + sourceAccount.getAccountNumber()
                + "', to='" + destinationAccount.getAccountNumber() + "'}";
    }
}
