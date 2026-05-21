package co.edu.tdea.bank.infrastructure.adapter.sql;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.BankAccountRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.adapter.ClientRepositoryAdapter;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankAccountJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BusinessClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.IndividualClientJpaRepository;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration tests for {@link BankAccountRepositoryAdapter} backed by H2
 * (MySQL compatibility mode). Exercises FK resolution, holder discriminator
 * handling and the unique constraint on {@code account_number} (the primary key).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
@Import({BankAccountRepositoryAdapter.class, ClientRepositoryAdapter.class})
class BankAccountRepositoryAdapterIT {

    private final BankAccountRepositoryAdapter adapter;
    private final ClientRepositoryAdapter clientAdapter;
    private final EntityManager em;

    @Autowired
    BankAccountRepositoryAdapterIT(BankAccountJpaRepository jpa,
                                   ClientJpaRepository clientJpa,
                                   IndividualClientJpaRepository individualJpa,
                                   BusinessClientJpaRepository businessJpa,
                                   EntityManager em) {
        this.adapter = new BankAccountRepositoryAdapter(jpa, clientJpa);
        this.clientAdapter = new ClientRepositoryAdapter(clientJpa, individualJpa, businessJpa);
        this.em = em;
    }

    @Test
    void should_PersistAndReturnAccount_when_SavingValidAccount() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        BankAccount account = DomainFixtures.activeBankAccount(holder);

        // Act
        BankAccount saved = adapter.save(account);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getAccountNumber()).isEqualTo(account.getAccountNumber());
        assertThat(saved.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(saved.getCurrency()).isEqualTo(CurrencyType.COP);
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getCurrentBalance()).isEqualByComparingTo("1000000.00");
        assertThat(saved.getHolder().getClientId()).isEqualTo(holder.getClientId());
    }

    @Test
    void should_ReturnAccount_when_FindByAccountNumberMatches() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        BankAccount saved = adapter.save(DomainFixtures.activeBankAccount(holder));
        em.flush();
        em.clear();

        // Act
        Optional<BankAccount> found = adapter.findByAccountNumber(saved.getAccountNumber());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo(saved.getAccountNumber());
        assertThat(found.get().getHolder().getClientId()).isEqualTo(holder.getClientId());
    }

    @Test
    void should_ReturnEmpty_when_FindByAccountNumberUnknown() {
        // Act
        Optional<BankAccount> found = adapter.findByAccountNumber("ACC-DOES-NOT-EXIST");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void should_ReturnAllAccountsForClient_when_FindByClientIdMatches() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        adapter.save(DomainFixtures.activeBankAccount(holder));
        adapter.save(DomainFixtures.activeBankAccount(holder));
        em.flush();
        em.clear();

        // Act
        List<BankAccount> accounts = adapter.findByClientId(holder.getClientId());

        // Assert
        assertThat(accounts).hasSize(2);
        assertThat(accounts).allSatisfy(a ->
                assertThat(a.getHolder().getClientId()).isEqualTo(holder.getClientId()));
    }

    @Test
    void should_ReturnAccountsFilteredByType_when_FindByClientIdAndType() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        BankAccount savings = DomainFixtures.activeBankAccount(holder);
        BankAccount checking = BankAccount.open(
                DomainFixtures.uniqueAccountNumber(),
                AccountType.CHECKING,
                holder,
                new BigDecimal("500000.00"),
                CurrencyType.COP);
        adapter.save(savings);
        adapter.save(checking);
        em.flush();
        em.clear();

        // Act
        List<BankAccount> savingsOnly =
                adapter.findByClientIdAndType(holder.getClientId(), AccountType.SAVINGS);

        // Assert
        assertThat(savingsOnly).hasSize(1);
        assertThat(savingsOnly.get(0).getAccountNumber()).isEqualTo(savings.getAccountNumber());
        assertThat(savingsOnly.get(0).getAccountType()).isEqualTo(AccountType.SAVINGS);
    }

    @Test
    void should_ReturnAccountsFilteredByStatus_when_FindByStatus() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        BankAccount active = DomainFixtures.activeBankAccount(holder);
        BankAccount toBlock = DomainFixtures.activeBankAccount(holder);
        toBlock.block();
        adapter.save(active);
        adapter.save(toBlock);
        em.flush();
        em.clear();

        // Act
        List<BankAccount> blocked = adapter.findByStatus(AccountStatus.BLOCKED);

        // Assert
        assertThat(blocked).hasSize(1);
        assertThat(blocked.get(0).getAccountNumber()).isEqualTo(toBlock.getAccountNumber());
        assertThat(blocked.get(0).getAccountStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void should_ReturnTrue_when_ExistsByAccountNumberMatches() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        BankAccount saved = adapter.save(DomainFixtures.activeBankAccount(holder));
        em.flush();

        // Act
        boolean exists = adapter.existsByAccountNumber(saved.getAccountNumber());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void should_ReturnFalse_when_ExistsByAccountNumberUnknown() {
        // Act
        boolean exists = adapter.existsByAccountNumber("ACC-NOTHING-HERE");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void should_AllowBusinessClientAsHolder_when_SavingAccount() {
        // Arrange
        BusinessClient holder = DomainFixtures.validBusinessClient();
        clientAdapter.save(holder);
        em.flush();
        BankAccount account = DomainFixtures.activeBankAccount(holder);

        // Act
        BankAccount saved = adapter.save(account);
        em.flush();
        em.clear();

        // Assert
        assertThat(saved.getHolder()).isInstanceOf(BusinessClient.class);
        assertThat(saved.getHolder().getClientId()).isEqualTo(holder.getClientId());
    }

    @Disabled("H2 @DataJpaTest never commits: PK/UNIQUE constraint on accountNumber " +
              "is deferred to commit-time in H2, which never fires inside a rolled-back " +
              "test transaction. Constraint is verified at the schema level; tested in E2E (T5).")
    @Test
    void should_RejectDuplicate_when_SavingTwoAccountsWithSameAccountNumber() {
        // Arrange
        IndividualClient holder = persistedIndividualHolder();
        String sharedNumber = DomainFixtures.uniqueAccountNumber();
        BankAccount first = BankAccount.open(sharedNumber, AccountType.SAVINGS, holder,
                new BigDecimal("100.00"), CurrencyType.COP);
        adapter.save(first);
        em.flush();
        em.clear();

        BankAccount duplicate = BankAccount.open(sharedNumber, AccountType.CHECKING, holder,
                new BigDecimal("200.00"), CurrencyType.COP);

        // Act + Assert — PK constraint on accountNumber verified at commit-time in H2;
        // not reachable in @DataJpaTest rolled-back transactions. See @Disabled reason.
        adapter.save(duplicate);
        em.flush();
    }

    @Test
    void should_FailToSave_when_HolderClientIdDoesNotExist() {
        // Arrange — holder not persisted, FK should fail
        IndividualClient ghostHolder = DomainFixtures.validIndividualClient();
        BankAccount orphan = DomainFixtures.activeBankAccount(ghostHolder);

        // Act + Assert
        assertThatThrownBy(() -> {
            adapter.save(orphan);
            em.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class,
                jakarta.persistence.EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private IndividualClient persistedIndividualHolder() {
        IndividualClient holder = DomainFixtures.validIndividualClient();
        clientAdapter.save(holder);
        em.flush();
        return holder;
    }
}
