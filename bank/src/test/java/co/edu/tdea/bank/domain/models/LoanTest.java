package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.LoanType;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    // -------------------------------------------------------------------------
    // Creation
    // -------------------------------------------------------------------------

    @Test
    void should_request_loan_when_all_fields_valid() {
        // Arrange
        Client applicant = DomainFixtures.validIndividualClient();

        // Act
        Loan loan = Loan.request(
                LoanType.PERSONAL,
                applicant,
                new BigDecimal("10000000"),
                36
        );

        // Assert
        assertThat(loan.getLoanId()).isNull();
        assertThat(loan.getLoanType()).isEqualTo(LoanType.PERSONAL);
        assertThat(loan.getApplicantClient()).isSameAs(applicant);
        assertThat(loan.getRequestedAmount()).isEqualByComparingTo("10000000");
        assertThat(loan.getTermInMonths()).isEqualTo(36);
        assertThat(loan.getLoanStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        assertThat(loan.getApprovedAmount()).isNull();
        assertThat(loan.getApprovalDate()).isNull();
    }

    @Test
    void should_throw_when_requestedAmount_is_zero_or_negative() {
        Client applicant = DomainFixtures.validIndividualClient();

        assertThatThrownBy(() -> Loan.request(LoanType.PERSONAL, applicant, BigDecimal.ZERO, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestedAmount");

        assertThatThrownBy(() -> Loan.request(LoanType.PERSONAL, applicant, new BigDecimal("-1"), 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestedAmount");
    }

    @Test
    void should_throw_when_termInMonths_is_zero_or_negative() {
        Client applicant = DomainFixtures.validIndividualClient();

        assertThatThrownBy(() -> Loan.request(LoanType.PERSONAL, applicant, new BigDecimal("100"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("termInMonths");

        assertThatThrownBy(() -> Loan.request(LoanType.PERSONAL, applicant, new BigDecimal("100"), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("termInMonths");
    }

    @Test
    void should_throw_when_applicantClient_is_null() {
        assertThatThrownBy(() -> Loan.request(LoanType.PERSONAL, null, new BigDecimal("100"), 12))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("applicantClient");
    }

    // -------------------------------------------------------------------------
    // approve()
    // -------------------------------------------------------------------------

    @Test
    void should_approve_loan_from_under_review() {
        // Arrange
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());
        LocalDate today = LocalDate.now();

        // Act
        loan.approve(new BigDecimal("4500000"), new BigDecimal("0.12"), today);

        // Assert
        assertThat(loan.getLoanStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(loan.getApprovedAmount()).isEqualByComparingTo("4500000");
        assertThat(loan.getInterestRate()).isEqualByComparingTo("0.12");
        assertThat(loan.getApprovalDate()).isEqualTo(today);
    }

    @Test
    void should_throw_when_approving_loan_not_in_under_review() {
        // Arrange — already REJECTED
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());
        loan.reject();

        // Act / Assert
        assertThatThrownBy(() -> loan.approve(new BigDecimal("100"), new BigDecimal("0.1"), LocalDate.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approve");
    }

    @Test
    void should_throw_when_approving_with_zero_or_negative_amount() {
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());

        assertThatThrownBy(() -> loan.approve(BigDecimal.ZERO, new BigDecimal("0.1"), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approvedAmount");
    }

    @Test
    void should_throw_when_approving_with_negative_interest_rate() {
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());

        assertThatThrownBy(() -> loan.approve(new BigDecimal("1000"), new BigDecimal("-0.01"), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interestRate");
    }

    @Test
    void should_throw_when_approving_with_future_approval_date() {
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());

        assertThatThrownBy(() -> loan.approve(new BigDecimal("1000"), new BigDecimal("0.1"), LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    // -------------------------------------------------------------------------
    // reject()
    // -------------------------------------------------------------------------

    @Test
    void should_reject_loan_from_under_review() {
        // Arrange
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());

        // Act
        loan.reject();

        // Assert
        assertThat(loan.getLoanStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    void should_throw_when_rejecting_loan_not_in_under_review() {
        // Arrange — already APPROVED
        Loan loan = DomainFixtures.validLoanUnderReview(DomainFixtures.validIndividualClient());
        loan.approve(new BigDecimal("1000"), new BigDecimal("0.1"), LocalDate.now());

        // Act / Assert
        assertThatThrownBy(loan::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reject");
    }

    // -------------------------------------------------------------------------
    // disburse()
    // -------------------------------------------------------------------------

    @Test
    void should_disburse_loan_from_approved() {
        // Arrange
        Client applicant = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        loan.approve(new BigDecimal("2000000"), new BigDecimal("0.1"), LocalDate.now());
        BankAccount target = DomainFixtures.activeBankAccount(applicant, BigDecimal.ZERO);

        // Act
        loan.disburse(LocalDate.now(), target);

        // Assert
        assertThat(loan.getLoanStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(loan.getDisbursementTargetAccount()).isSameAs(target);
        assertThat(loan.getDisbursementDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void should_throw_when_disbursing_loan_not_in_approved() {
        // Arrange — still UNDER_REVIEW
        Client applicant = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        BankAccount target = DomainFixtures.activeBankAccount(applicant);

        // Act / Assert
        assertThatThrownBy(() -> loan.disburse(LocalDate.now(), target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disburse");
    }

    @Test
    void should_throw_when_disbursement_account_is_not_active() {
        // Arrange
        Client applicant = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        loan.approve(new BigDecimal("1000"), new BigDecimal("0.1"), LocalDate.now());

        BankAccount target = DomainFixtures.activeBankAccount(applicant);
        target.block();

        // Act / Assert
        assertThatThrownBy(() -> loan.disburse(LocalDate.now(), target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void should_throw_when_disbursement_date_is_in_the_future() {
        Client applicant = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        loan.approve(new BigDecimal("1000"), new BigDecimal("0.1"), LocalDate.now());
        BankAccount target = DomainFixtures.activeBankAccount(applicant);

        assertThatThrownBy(() -> loan.disburse(LocalDate.now().plusDays(1), target))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void should_increase_account_balance_after_disbursement() {
        // Arrange
        Client applicant = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        BigDecimal approved = new BigDecimal("3000000");
        loan.approve(approved, new BigDecimal("0.12"), LocalDate.now());

        BankAccount target = DomainFixtures.activeBankAccount(applicant, new BigDecimal("100000"));
        BigDecimal balanceBefore = target.getCurrentBalance();

        // Act
        loan.disburse(LocalDate.now(), target);

        // Assert
        assertThat(target.getCurrentBalance()).isEqualByComparingTo(balanceBefore.add(approved));
    }

    // -------------------------------------------------------------------------
    // reconstruct() coherence
    // -------------------------------------------------------------------------

    @Test
    void should_reconstruct_under_review_loan_with_minimal_fields() {
        // Arrange
        Client applicant = DomainFixtures.validIndividualClient();

        // Act
        Loan loan = Loan.reconstruct(
                10L,
                LoanType.PERSONAL,
                applicant,
                new BigDecimal("1000"),
                12,
                null,
                null,
                LoanStatus.UNDER_REVIEW,
                null,
                null,
                null
        );

        // Assert
        assertThat(loan.getLoanStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        assertThat(loan.getApprovedAmount()).isNull();
    }

    @Test
    void should_throw_when_reconstructing_approved_loan_without_approvalDate() {
        Client applicant = DomainFixtures.validIndividualClient();

        assertThatThrownBy(() -> Loan.reconstruct(
                11L,
                LoanType.PERSONAL,
                applicant,
                new BigDecimal("1000"),
                12,
                new BigDecimal("900"),
                new BigDecimal("0.1"),
                LoanStatus.APPROVED,
                null,                       // approvalDate missing — invariant violation
                null,
                null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("approvalDate");
    }

    @Test
    void should_throw_when_reconstructing_disbursed_loan_without_disbursementDate() {
        Client applicant = DomainFixtures.validIndividualClient();
        BankAccount target = DomainFixtures.activeBankAccount(applicant);

        assertThatThrownBy(() -> Loan.reconstruct(
                12L,
                LoanType.PERSONAL,
                applicant,
                new BigDecimal("1000"),
                12,
                new BigDecimal("900"),
                new BigDecimal("0.1"),
                LoanStatus.DISBURSED,
                LocalDate.now().minusDays(1),
                null,
                target
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("disbursementDate");
    }
}
