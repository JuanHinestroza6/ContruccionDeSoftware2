package co.edu.tdea.bank.testfixtures;

import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.domain.enums.LoanType;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Centralized factory of valid domain instances for tests.
 * Each method returns a domain object built through the regular factory methods,
 * so the produced fixture always satisfies every invariant defined in the model.
 */
public final class DomainFixtures {

    private DomainFixtures() {}

    // ---------------------------------------------------------------------
    // Clients
    // ---------------------------------------------------------------------

    public static IndividualClient validIndividualClient() {
        return IndividualClient.create(
                "1001234567",
                "jane.doe@example.com",
                "+57 300 1234567",
                "Cra 50 # 10-20, Medellin",
                "Jane Doe",
                LocalDate.now().minusYears(30)
        );
    }

    public static BusinessClient validBusinessClient() {
        return BusinessClient.create(
                "900123456-7",
                "contact@acme.co",
                "+57 604 1234567",
                "Cl 1 # 2-3, Medellin",
                "ACME S.A.S.",
                "John Doe"
        );
    }

    // ---------------------------------------------------------------------
    // Accounts
    // ---------------------------------------------------------------------

    /** Returns a freshly-opened ACTIVE account with a positive opening balance. */
    public static BankAccount activeBankAccount(Client holder) {
        return BankAccount.open(
                uniqueAccountNumber(),
                AccountType.SAVINGS,
                holder,
                new BigDecimal("1000000.00"),
                CurrencyType.COP
        );
    }

    /** Returns a freshly-opened ACTIVE account with a caller-supplied opening balance. */
    public static BankAccount activeBankAccount(Client holder, BigDecimal initialBalance) {
        return BankAccount.open(
                uniqueAccountNumber(),
                AccountType.SAVINGS,
                holder,
                initialBalance,
                CurrencyType.COP
        );
    }

    public static String uniqueAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------------

    public static User validUser() {
        return User.builder()
                .userId(UUID.randomUUID())
                .fullName("Mary Manager")
                .identificationId("U-" + UUID.randomUUID().toString().substring(0, 8))
                .email("mary.manager@example.com")
                .phone("+57 300 7654321")
                .birthDate(LocalDate.of(1990, 5, 12))
                .address("Cl 80 # 65-12, Medellin")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    /** Internal-employee-style user without birthDate (allowed by the domain). */
    public static User validInternalUserWithoutBirthDate() {
        return User.builder()
                .userId(UUID.randomUUID())
                .fullName("Teller Bob")
                .identificationId("U-" + UUID.randomUUID().toString().substring(0, 8))
                .email("teller.bob@example.com")
                .phone("+57 300 1112222")
                .address("Branch HQ, Medellin")
                .systemRole(SystemRole.TELLER_EMPLOYEE)
                .build();
    }

    // ---------------------------------------------------------------------
    // Loans
    // ---------------------------------------------------------------------

    public static Loan validLoanUnderReview(Client applicant) {
        return Loan.request(
                LoanType.PERSONAL,
                applicant,
                new BigDecimal("5000000"),
                24
        );
    }

    // ---------------------------------------------------------------------
    // Transfers
    // ---------------------------------------------------------------------

    public static Transfer validPendingTransfer(BankAccount source,
                                                BankAccount destination,
                                                User createdBy) {
        return Transfer.create(
                source,
                destination,
                new BigDecimal("100000"),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                createdBy
        );
    }

    public static Transfer validPendingTransfer(BankAccount source,
                                                BankAccount destination,
                                                BigDecimal amount,
                                                User createdBy) {
        return Transfer.create(
                source,
                destination,
                amount,
                LocalDateTime.of(2026, 1, 1, 10, 0),
                createdBy
        );
    }

    public static Transfer validPendingTransfer(BankAccount source,
                                                BankAccount destination,
                                                BigDecimal amount,
                                                LocalDateTime createdAt,
                                                User createdBy) {
        return Transfer.create(
                source,
                destination,
                amount,
                createdAt,
                createdBy
        );
    }
}
