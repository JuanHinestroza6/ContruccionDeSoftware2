package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.LoanType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_applicant_client_id", columnList = "applicant_client_id"),
        @Index(name = "idx_loans_loan_status", columnList = "loan_status")
})
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "applicant_client_id", nullable = false)
    private ClientEntity applicantClient;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Column(name = "term_in_months", nullable = false)
    private int termInMonths;

    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(name = "interest_rate", precision = 19, scale = 4)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_status", nullable = false)
    private LoanStatus loanStatus;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "disbursement_target_account_number", referencedColumnName = "account_number")
    private BankAccountEntity disbursementTargetAccount;
}
