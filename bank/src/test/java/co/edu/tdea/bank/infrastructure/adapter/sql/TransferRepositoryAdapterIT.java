package co.edu.tdea.bank.infrastructure.adapter.sql;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.BankAccountRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.ClientRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.TransferRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankAccountJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BusinessClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.IndividualClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.TransferJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration tests for {@link TransferRepositoryAdapter} backed by H2 (MySQL
 * compatibility mode). Verifies FK resolution through Hibernate proxies
 * (source &amp; destination accounts, createdBy, optional approvedBy) and
 * query methods.
 *
 * <p>Note: users are persisted directly through {@link UserJpaRepository} so
 * the test can supply the security fields (username/password) that the domain
 * mapper intentionally does NOT populate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
@Import({TransferRepositoryAdapter.class, ClientRepositoryAdapter.class,
        BankAccountRepositoryAdapter.class})
class TransferRepositoryAdapterIT {

    private final TransferRepositoryAdapter adapter;
    private final ClientRepositoryAdapter clientAdapter;
    private final BankAccountRepositoryAdapter accountAdapter;
    private final UserJpaRepository userJpa;
    private final EntityManager em;

    @Autowired
    TransferRepositoryAdapterIT(TransferJpaRepository jpa,
                                BankAccountJpaRepository bankAccountJpa,
                                UserJpaRepository userJpa,
                                ClientJpaRepository clientJpa,
                                IndividualClientJpaRepository individualJpa,
                                BusinessClientJpaRepository businessJpa,
                                EntityManager em) {
        this.adapter = new TransferRepositoryAdapter(jpa, bankAccountJpa, userJpa);
        this.clientAdapter = new ClientRepositoryAdapter(clientJpa, individualJpa, businessJpa);
        this.accountAdapter = new BankAccountRepositoryAdapter(bankAccountJpa, clientJpa);
        this.userJpa = userJpa;
        this.em = em;
    }

    @Test
    void should_PersistAndAssignTransferId_when_SavingPendingTransfer() {
        // Arrange
        Fixture f = persistedFixture();
        Transfer transfer = DomainFixtures.validPendingTransfer(f.source, f.destination, f.user);

        // Act
        Transfer saved = adapter.save(transfer);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getTransferId()).isNotNull().isPositive();
        assertThat(saved.getTransferStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
        assertThat(saved.getAmount()).isEqualByComparingTo("100000");
        assertThat(saved.getSourceAccount().getAccountNumber())
                .isEqualTo(f.source.getAccountNumber());
        assertThat(saved.getDestinationAccount().getAccountNumber())
                .isEqualTo(f.destination.getAccountNumber());
        assertThat(saved.getCreatedBy().getUserId()).isEqualTo(f.user.getUserId());
        assertThat(saved.getApprovedBy()).isNull();
    }

    @Test
    void should_PersistExecutedTransfer_when_TransferWasApprovedBeforeSaving() {
        // Arrange
        Fixture f = persistedFixture();
        Transfer transfer = DomainFixtures.validPendingTransfer(
                f.source, f.destination, new BigDecimal("50000"), f.user);
        User approver = persistedUser("alice.approver@example.com",
                SystemRole.INTERNAL_ANALYST);
        transfer.approve(approver, LocalDateTime.of(2026, 1, 1, 11, 0));

        // Act
        Transfer saved = adapter.save(transfer);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getTransferStatus()).isEqualTo(TransferStatus.EXECUTED);
        assertThat(saved.getApprovedBy()).isNotNull();
        assertThat(saved.getApprovedBy().getUserId()).isEqualTo(approver.getUserId());
        assertThat(saved.getApprovalDateTime()).isEqualTo(LocalDateTime.of(2026, 1, 1, 11, 0));
    }

    @Test
    void should_ReturnTransfer_when_FindByIdMatches() {
        // Arrange
        Fixture f = persistedFixture();
        Transfer saved = adapter.save(
                DomainFixtures.validPendingTransfer(f.source, f.destination, f.user));
        em.flush();
        em.clear();

        // Act
        Optional<Transfer> found = adapter.findById(saved.getTransferId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTransferId()).isEqualTo(saved.getTransferId());
    }

    @Test
    void should_ReturnEmpty_when_FindByIdUnknown() {
        // Act
        Optional<Transfer> found = adapter.findById(999_999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void should_ReturnTransfers_when_FindBySourceAccountNumberMatches() {
        // Arrange
        Fixture f = persistedFixture();
        adapter.save(DomainFixtures.validPendingTransfer(f.source, f.destination, f.user));
        adapter.save(DomainFixtures.validPendingTransfer(f.source, f.destination, f.user));
        em.flush();
        em.clear();

        // Act
        List<Transfer> bySource = adapter.findBySourceAccountNumber(f.source.getAccountNumber());

        // Assert
        assertThat(bySource).hasSize(2);
        assertThat(bySource).allSatisfy(t ->
                assertThat(t.getSourceAccount().getAccountNumber())
                        .isEqualTo(f.source.getAccountNumber()));
    }

    @Test
    void should_ReturnTransfers_when_FindByDestinationAccountNumberMatches() {
        // Arrange
        Fixture f = persistedFixture();
        adapter.save(DomainFixtures.validPendingTransfer(f.source, f.destination, f.user));
        em.flush();
        em.clear();

        // Act
        List<Transfer> byDestination =
                adapter.findByDestinationAccountNumber(f.destination.getAccountNumber());

        // Assert
        assertThat(byDestination).hasSize(1);
        assertThat(byDestination.get(0).getDestinationAccount().getAccountNumber())
                .isEqualTo(f.destination.getAccountNumber());
    }

    @Test
    void should_ReturnTransfersFilteredByStatus_when_FindByStatus() {
        // Arrange
        Fixture f = persistedFixture();
        Transfer pending = DomainFixtures.validPendingTransfer(f.source, f.destination, f.user);
        Transfer toReject = DomainFixtures.validPendingTransfer(
                f.source, f.destination, new BigDecimal("10000"), f.user);
        User approver = persistedUser("rej@example.com", SystemRole.INTERNAL_ANALYST);
        toReject.reject(approver);
        adapter.save(pending);
        adapter.save(toReject);
        em.flush();
        em.clear();

        // Act
        List<Transfer> rejected = adapter.findByStatus(TransferStatus.REJECTED);

        // Assert
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getTransferStatus()).isEqualTo(TransferStatus.REJECTED);
    }

    @Test
    void should_ReturnTransfersForCreator_when_FindByCreatedByUserId() {
        // Arrange
        Fixture f = persistedFixture();
        adapter.save(DomainFixtures.validPendingTransfer(f.source, f.destination, f.user));
        em.flush();
        em.clear();

        // Act
        List<Transfer> byCreator = adapter.findByCreatedByUserId(f.user.getUserId());

        // Assert
        assertThat(byCreator).hasSize(1);
        assertThat(byCreator.get(0).getCreatedBy().getUserId()).isEqualTo(f.user.getUserId());
    }

    @Test
    void should_ReturnEmptyList_when_FindByCreatedByUserIdUnknown() {
        // Act
        List<Transfer> byCreator = adapter.findByCreatedByUserId(UUID.randomUUID());

        // Assert
        assertThat(byCreator).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test fixture wiring
    // -------------------------------------------------------------------------

    private record Fixture(IndividualClient holder,
                           BankAccount source,
                           BankAccount destination,
                           User user) {}

    private Fixture persistedFixture() {
        IndividualClient holder = DomainFixtures.validIndividualClient();
        clientAdapter.save(holder);
        em.flush();
        BankAccount source = accountAdapter.save(DomainFixtures.activeBankAccount(holder));
        BankAccount destination = accountAdapter.save(DomainFixtures.activeBankAccount(holder));
        User user = persistedUser("creator-" + UUID.randomUUID() + "@example.com",
                SystemRole.COMMERCIAL_EMPLOYEE);
        em.flush();
        return new Fixture(holder, source, destination, user);
    }

    private User persistedUser(String email, SystemRole role) {
        // UserMapper.toEntity intentionally does NOT populate username/password
        // (security fields owned by the auth adapter). For integration tests
        // we go straight to the JPA repo so the NOT NULL columns are satisfied.
        UserEntity entity = new UserEntity();
        UUID userId = UUID.randomUUID();
        entity.setUserId(userId);
        entity.setUsername("user-" + userId);
        entity.setPassword("secret");
        entity.setSystemRole(role);
        entity.setUserStatus(UserStatus.ACTIVE);
        entity.setFullName("Test User");
        entity.setIdentificationId("U-" + userId.toString().substring(0, 8));
        entity.setEmail(email);
        entity.setPhone("+57 300 0000000");
        entity.setBirthDate(LocalDate.of(1990, 1, 1));
        entity.setAddress("Some address");
        userJpa.save(entity);
        em.flush();
        return User.builder()
                .userId(userId)
                .fullName(entity.getFullName())
                .identificationId(entity.getIdentificationId())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .birthDate(entity.getBirthDate())
                .address(entity.getAddress())
                .systemRole(role)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }
}
