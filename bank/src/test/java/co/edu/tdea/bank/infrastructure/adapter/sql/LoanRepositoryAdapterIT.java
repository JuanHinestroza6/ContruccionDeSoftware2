package co.edu.tdea.bank.infrastructure.adapter.sql;

import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.BankAccountRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.ClientRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.LoanRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankAccountJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BusinessClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.IndividualClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.LoanJpaRepository;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration tests for {@link LoanRepositoryAdapter} backed by H2 (MySQL
 * compatibility mode). Covers persistence with FK references to a client (and
 * an optional disbursement target account), plus queries by status/client.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
@Import({LoanRepositoryAdapter.class, ClientRepositoryAdapter.class,
        BankAccountRepositoryAdapter.class})
class LoanRepositoryAdapterIT {

    private final LoanRepositoryAdapter adapter;
    private final ClientRepositoryAdapter clientAdapter;
    private final BankAccountRepositoryAdapter accountAdapter;
    private final EntityManager em;

    @Autowired
    LoanRepositoryAdapterIT(LoanJpaRepository jpa,
                            ClientJpaRepository clientJpa,
                            IndividualClientJpaRepository individualJpa,
                            BusinessClientJpaRepository businessJpa,
                            BankAccountJpaRepository bankAccountJpa,
                            EntityManager em) {
        this.adapter = new LoanRepositoryAdapter(jpa, clientJpa, bankAccountJpa);
        this.clientAdapter = new ClientRepositoryAdapter(clientJpa, individualJpa, businessJpa);
        this.accountAdapter = new BankAccountRepositoryAdapter(bankAccountJpa, clientJpa);
        this.em = em;
    }

    @Test
    void should_PersistAndAssignLoanId_when_SavingLoanUnderReview() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);

        // Act
        Loan saved = adapter.save(loan);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getLoanId()).isNotNull().isPositive();
        assertThat(saved.getLoanStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
        assertThat(saved.getRequestedAmount()).isEqualByComparingTo("5000000");
        assertThat(saved.getApplicantClient().getClientId()).isEqualTo(applicant.getClientId());
        assertThat(saved.getDisbursementTargetAccount()).isNull();
    }

    @Test
    void should_PersistApprovedLoan_when_LoanWasApprovedBeforeSaving() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        loan.approve(new BigDecimal("4000000"), new BigDecimal("0.15"),
                LocalDate.of(2026, 1, 15));

        // Act
        Loan saved = adapter.save(loan);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getLoanStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(saved.getApprovedAmount()).isEqualByComparingTo("4000000");
        assertThat(saved.getInterestRate()).isEqualByComparingTo("0.15");
        assertThat(saved.getApprovalDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void should_PersistDisbursedLoanWithTarget_when_LoanWasDisbursedBeforeSaving() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        BankAccount target = accountAdapter.save(DomainFixtures.activeBankAccount(applicant));
        em.flush();

        Loan loan = DomainFixtures.validLoanUnderReview(applicant);
        loan.approve(new BigDecimal("2000000"), new BigDecimal("0.10"),
                LocalDate.of(2026, 1, 10));
        loan.disburse(LocalDate.of(2026, 1, 12), target);

        // Act
        Loan saved = adapter.save(loan);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getLoanStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(saved.getDisbursementDate()).isEqualTo(LocalDate.of(2026, 1, 12));
        assertThat(saved.getDisbursementTargetAccount()).isNotNull();
        assertThat(saved.getDisbursementTargetAccount().getAccountNumber())
                .isEqualTo(target.getAccountNumber());
    }

    @Test
    void should_ReturnLoan_when_FindByIdMatches() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        Loan saved = adapter.save(DomainFixtures.validLoanUnderReview(applicant));
        em.flush();
        em.clear();

        // Act
        Optional<Loan> found = adapter.findById(saved.getLoanId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getLoanId()).isEqualTo(saved.getLoanId());
        assertThat(found.get().getApplicantClient().getClientId())
                .isEqualTo(applicant.getClientId());
    }

    @Test
    void should_ReturnEmpty_when_FindByIdUnknown() {
        // Act
        Optional<Loan> found = adapter.findById(999_999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void should_ReturnAllLoansForClient_when_FindByClientIdMatches() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        adapter.save(DomainFixtures.validLoanUnderReview(applicant));
        adapter.save(DomainFixtures.validLoanUnderReview(applicant));
        em.flush();
        em.clear();

        // Act
        List<Loan> loans = adapter.findByClientId(applicant.getClientId());

        // Assert
        assertThat(loans).hasSize(2);
        assertThat(loans).allSatisfy(l ->
                assertThat(l.getApplicantClient().getClientId())
                        .isEqualTo(applicant.getClientId()));
    }

    @Test
    void should_ReturnLoansFilteredByStatus_when_FindByStatus() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        Loan underReview = DomainFixtures.validLoanUnderReview(applicant);
        Loan toReject = DomainFixtures.validLoanUnderReview(applicant);
        toReject.reject();
        adapter.save(underReview);
        adapter.save(toReject);
        em.flush();
        em.clear();

        // Act
        List<Loan> rejected = adapter.findByStatus(LoanStatus.REJECTED);

        // Assert
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getLoanStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    void should_ReturnLoansFilteredByClientAndStatus_when_FindByClientIdAndStatus() {
        // Arrange
        IndividualClient applicant = persistedApplicant();
        Loan underReview = DomainFixtures.validLoanUnderReview(applicant);
        Loan toReject = DomainFixtures.validLoanUnderReview(applicant);
        toReject.reject();
        adapter.save(underReview);
        adapter.save(toReject);
        em.flush();
        em.clear();

        // Act
        List<Loan> underReviewLoans = adapter.findByClientIdAndStatus(
                applicant.getClientId(), LoanStatus.UNDER_REVIEW);

        // Assert
        assertThat(underReviewLoans).hasSize(1);
        assertThat(underReviewLoans.get(0).getLoanStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
    }

    @Test
    void should_ReturnEmptyList_when_FindByClientIdHasNoLoans() {
        // Arrange
        IndividualClient applicant = persistedApplicant();

        // Act
        List<Loan> loans = adapter.findByClientId(applicant.getClientId());

        // Assert
        assertThat(loans).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private IndividualClient persistedApplicant() {
        IndividualClient client = DomainFixtures.validIndividualClient();
        clientAdapter.save(client);
        em.flush();
        return client;
    }
}
