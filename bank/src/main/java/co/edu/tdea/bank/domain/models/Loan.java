package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Aggregate root that represents a loan request and its lifecycle.
 *
 * <p>State machine:
 * <pre>
 *   UNDER_REVIEW ──► APPROVED ──► DISBURSED
 *        │
 *        └──────────► REJECTED
 * </pre>
 *
 * <p>Invariants enforced at all times:
 * <ul>
 *   <li>A loan is created in {@code UNDER_REVIEW}; no other initial state is allowed.</li>
 *   <li>{@code approve()} is only valid from {@code UNDER_REVIEW}.</li>
 *   <li>{@code reject()} is only valid from {@code UNDER_REVIEW}.</li>
 *   <li>{@code disburse()} is only valid from {@code APPROVED}.</li>
 *   <li>The disbursement target account must be {@code ACTIVE} at disbursal time.</li>
 *   <li>{@code disburse()} credits {@code approvedAmount} into the target account.</li>
 * </ul>
 */
public final class Loan {

    private final Long loanId;
    private final LoanType loanType;
    private final Client applicantClient;
    private final BigDecimal requestedAmount;
    private final Integer termInMonths;

    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private LoanStatus loanStatus;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private BankAccount disbursementTargetAccount;

    private Loan(Long loanId, LoanType loanType, Client applicantClient,
                 BigDecimal requestedAmount, Integer termInMonths) {
        this.loanId           = loanId;
        this.loanType         = loanType;
        this.applicantClient  = applicantClient;
        this.requestedAmount  = requestedAmount;
        this.termInMonths     = termInMonths;
        this.loanStatus       = LoanStatus.UNDER_REVIEW;
    }

    /**
     * Factory method — validates all invariants and creates a loan in {@code UNDER_REVIEW}.
     *
     * @param loanId          unique identifier for the loan
     * @param loanType        product type being requested
     * @param applicantClient client submitting the request (never null)
     * @param requestedAmount amount the client wishes to borrow; must be > 0
     * @param termInMonths    repayment term in months; must be > 0
     */
    public static Loan request(Long loanId,
                               LoanType loanType,
                               Client applicantClient,
                               BigDecimal requestedAmount,
                               Integer termInMonths) {
        Objects.requireNonNull(loanId,           "loanId must not be null.");
        Objects.requireNonNull(loanType,         "loanType must not be null.");
        Objects.requireNonNull(applicantClient,  "applicantClient must not be null.");
        Objects.requireNonNull(requestedAmount,  "requestedAmount must not be null.");
        Objects.requireNonNull(termInMonths,     "termInMonths must not be null.");

        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "requestedAmount must be > 0, got: " + requestedAmount);
        }
        if (termInMonths <= 0) {
            throw new IllegalArgumentException(
                    "termInMonths must be > 0, got: " + termInMonths);
        }

        return new Loan(loanId, loanType, applicantClient, requestedAmount, termInMonths);
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     *
     * <p>Validaciones mínimas de coherencia de estado:
     * <ul>
     *   <li>{@code APPROVED}/{@code DISBURSED} ⇒ {@code approvalDate}, {@code approvedAmount},
     *       {@code interestRate} no nulos.</li>
     *   <li>{@code DISBURSED} ⇒ {@code disbursementDate} y {@code disbursementTargetAccount} no nulos.</li>
     * </ul>
     */
    public static Loan reconstruct(Long loanId,
                                   LoanType loanType,
                                   Client applicantClient,
                                   BigDecimal requestedAmount,
                                   Integer termInMonths,
                                   BigDecimal approvedAmount,
                                   BigDecimal interestRate,
                                   LoanStatus loanStatus,
                                   LocalDate approvalDate,
                                   LocalDate disbursementDate,
                                   BankAccount disbursementTargetAccount) {
        Objects.requireNonNull(loanId,          "loanId must not be null.");
        Objects.requireNonNull(loanType,        "loanType must not be null.");
        Objects.requireNonNull(applicantClient, "applicantClient must not be null.");
        Objects.requireNonNull(requestedAmount, "requestedAmount must not be null.");
        Objects.requireNonNull(termInMonths,    "termInMonths must not be null.");
        Objects.requireNonNull(loanStatus,      "loanStatus must not be null.");

        if (loanStatus == LoanStatus.APPROVED || loanStatus == LoanStatus.DISBURSED) {
            Objects.requireNonNull(approvedAmount, "approvedAmount must not be null when status is " + loanStatus + ".");
            Objects.requireNonNull(interestRate,   "interestRate must not be null when status is " + loanStatus + ".");
            Objects.requireNonNull(approvalDate,   "approvalDate must not be null when status is " + loanStatus + ".");
        }
        if (loanStatus == LoanStatus.DISBURSED) {
            Objects.requireNonNull(disbursementDate,          "disbursementDate must not be null when status is DISBURSED.");
            Objects.requireNonNull(disbursementTargetAccount, "disbursementTargetAccount must not be null when status is DISBURSED.");
        }

        Loan loan = new Loan(loanId, loanType, applicantClient, requestedAmount, termInMonths);
        loan.approvedAmount            = approvedAmount;
        loan.interestRate              = interestRate;
        loan.loanStatus                = loanStatus;
        loan.approvalDate              = approvalDate;
        loan.disbursementDate          = disbursementDate;
        loan.disbursementTargetAccount = disbursementTargetAccount;
        return loan;
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    /**
     * Approves the loan with the given commercial terms.
     *
     * @param approvedAmount amount the bank agrees to lend; must be > 0
     * @param interestRate   annual interest rate applied; must be >= 0
     * @param approvalDate   date of the approval decision; must not be null or future
     * @throws IllegalStateException    if the loan is not in {@code UNDER_REVIEW}
     * @throws IllegalArgumentException if any monetary argument violates its constraint
     */
    public void approve(BigDecimal approvedAmount, BigDecimal interestRate, LocalDate approvalDate) {
        requireStatus(LoanStatus.UNDER_REVIEW, "approve");

        Objects.requireNonNull(approvedAmount, "approvedAmount must not be null.");
        Objects.requireNonNull(interestRate,   "interestRate must not be null.");
        Objects.requireNonNull(approvalDate,   "approvalDate must not be null.");

        if (approvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "approvedAmount must be > 0, got: " + approvedAmount);
        }
        if (interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "interestRate must be >= 0, got: " + interestRate);
        }
        if (approvalDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "approvalDate must not be a future date, got: " + approvalDate);
        }

        this.approvedAmount = approvedAmount;
        this.interestRate   = interestRate;
        this.approvalDate   = approvalDate;
        this.loanStatus     = LoanStatus.APPROVED;
    }

    /**
     * Rejects the loan request. No further state transition is possible after rejection.
     *
     * @throws IllegalStateException if the loan is not in {@code UNDER_REVIEW}
     */
    public void reject() {
        requireStatus(LoanStatus.UNDER_REVIEW, "reject");
        this.loanStatus = LoanStatus.REJECTED;
    }

    /**
     * Disburses the loan by crediting {@code approvedAmount} into the target account.
     *
     * @param disbursementDate      date the funds are transferred; must not be null or future
     * @param disbursementTargetAccount the {@link BankAccount} that will receive the funds;
     *                              must be non-null and in {@code ACTIVE} status
     * @throws IllegalStateException    if the loan is not in {@code APPROVED},
     *                                  or if the target account is not {@code ACTIVE}
     */
    public void disburse(LocalDate disbursementDate, BankAccount disbursementTargetAccount) {
        requireStatus(LoanStatus.APPROVED, "disburse");

        Objects.requireNonNull(disbursementDate,          "disbursementDate must not be null.");
        Objects.requireNonNull(disbursementTargetAccount, "disbursementTargetAccount must not be null.");

        if (disbursementDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "disbursementDate must not be a future date, got: " + disbursementDate);
        }
        if (disbursementTargetAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Disbursement target account must be ACTIVE, but is: "
                    + disbursementTargetAccount.getAccountStatus());
        }

        disbursementTargetAccount.deposit(approvedAmount);

        this.disbursementTargetAccount = disbursementTargetAccount;
        this.disbursementDate          = disbursementDate;
        this.loanStatus                = LoanStatus.DISBURSED;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getLoanId()                              { return loanId; }
    public LoanType getLoanType()                        { return loanType; }
    public Client getApplicantClient()                   { return applicantClient; }
    public BigDecimal getRequestedAmount()               { return requestedAmount; }
    public BigDecimal getApprovedAmount()                { return approvedAmount; }
    public BigDecimal getInterestRate()                  { return interestRate; }
    public Integer getTermInMonths()                     { return termInMonths; }
    public LoanStatus getLoanStatus()                    { return loanStatus; }
    public LocalDate getApprovalDate()                   { return approvalDate; }
    public LocalDate getDisbursementDate()               { return disbursementDate; }
    public BankAccount getDisbursementTargetAccount()    { return disbursementTargetAccount; }

    // -------------------------------------------------------------------------
    // Private guard
    // -------------------------------------------------------------------------

    private void requireStatus(LoanStatus required, String operation) {
        if (loanStatus != required) {
            throw new IllegalStateException(
                    "Cannot " + operation + ": loan is " + loanStatus
                    + ", expected " + required + ".");
        }
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan loan)) return false;
        return Objects.equals(loanId, loan.loanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

    @Override
    public String toString() {
        return "Loan{loanId=" + loanId + ", type=" + loanType
                + ", status=" + loanStatus + ", requestedAmount=" + requestedAmount + '}';
    }
}
