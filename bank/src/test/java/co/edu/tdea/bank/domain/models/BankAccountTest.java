package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankAccountTest {

    @Test
    void should_open_account_when_all_fields_valid() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();

        // Act
        BankAccount account = BankAccount.open(
                "ACC-0001",
                AccountType.SAVINGS,
                holder,
                new BigDecimal("250000"),
                CurrencyType.COP
        );

        // Assert
        assertThat(account.getAccountNumber()).isEqualTo("ACC-0001");
        assertThat(account.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getHolder()).isSameAs(holder);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("250000");
        assertThat(account.getCurrency()).isEqualTo(CurrencyType.COP);
        assertThat(account.getOpeningDate()).isEqualTo(LocalDate.now());
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.isActive()).isTrue();
        assertThat(account.canOperate()).isTrue();
    }

    @Test
    void should_throw_when_accountNumber_is_blank() {
        Client holder = DomainFixtures.validIndividualClient();

        assertThatThrownBy(() -> BankAccount.open(
                "   ",
                AccountType.SAVINGS,
                holder,
                BigDecimal.ZERO,
                CurrencyType.COP
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountNumber");
    }

    @Test
    void should_throw_when_initialBalance_is_negative() {
        Client holder = DomainFixtures.validIndividualClient();

        assertThatThrownBy(() -> BankAccount.open(
                "ACC-0001",
                AccountType.SAVINGS,
                holder,
                new BigDecimal("-1"),
                CurrencyType.COP
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initialBalance");
    }

    @Test
    void should_throw_when_holder_is_null() {
        assertThatThrownBy(() -> BankAccount.open(
                "ACC-0001",
                AccountType.SAVINGS,
                null,
                BigDecimal.ZERO,
                CurrencyType.COP
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("holder");
    }

    @Test
    void should_deposit_and_increase_balance() {
        // Arrange
        BankAccount account = BankAccount.open(
                "ACC-0001",
                AccountType.SAVINGS,
                DomainFixtures.validIndividualClient(),
                new BigDecimal("1000"),
                CurrencyType.COP
        );

        // Act
        account.deposit(new BigDecimal("500"));

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("1500");
    }

    @Test
    void should_throw_when_deposit_amount_is_zero_or_negative() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());

        // Act / Assert — zero
        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deposit");

        // Act / Assert — negative
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deposit");
    }

    @Test
    void should_throw_when_deposit_amount_is_null() {
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());

        assertThatThrownBy(() -> account.deposit(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("deposit");
    }

    @Test
    void should_withdraw_and_decrease_balance() {
        // Arrange
        BankAccount account = BankAccount.open(
                "ACC-0001",
                AccountType.CHECKING,
                DomainFixtures.validIndividualClient(),
                new BigDecimal("1000"),
                CurrencyType.COP
        );

        // Act
        account.withdraw(new BigDecimal("300"));

        // Assert
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("700");
    }

    @Test
    void should_throw_when_insufficient_balance() {
        // Arrange
        BankAccount account = BankAccount.open(
                "ACC-0001",
                AccountType.SAVINGS,
                DomainFixtures.validIndividualClient(),
                new BigDecimal("100"),
                CurrencyType.COP
        );

        // Act / Assert
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("101")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");

        // Balance unchanged
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("100");
    }

    @Test
    void should_throw_when_withdraw_amount_is_zero_or_negative() {
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());

        assertThatThrownBy(() -> account.withdraw(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("withdrawal");

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("-50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("withdrawal");
    }

    @Test
    void should_throw_when_account_is_blocked_and_operation_attempted() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());
        account.block();

        // Act / Assert — deposit
        assertThatThrownBy(() -> account.deposit(new BigDecimal("100")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BLOCKED");

        // Act / Assert — withdraw
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void should_throw_when_account_is_canceled_and_operation_attempted() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());
        account.cancel();

        // Act / Assert
        assertThatThrownBy(() -> account.deposit(new BigDecimal("100")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELED");
    }

    @Test
    void should_block_active_account() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());

        // Act
        account.block();

        // Assert
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.BLOCKED);
        assertThat(account.canOperate()).isFalse();
        assertThat(account.isActive()).isFalse();
    }

    @Test
    void should_cancel_active_account() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());

        // Act
        account.cancel();

        // Assert
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.CANCELED);
        assertThat(account.canOperate()).isFalse();
    }

    @Test
    void should_throw_when_canceling_already_canceled_account() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());
        account.cancel();

        // Act / Assert
        assertThatThrownBy(account::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already canceled");
    }

    @Test
    void should_throw_when_blocking_already_canceled_account() {
        // Arrange
        BankAccount account = DomainFixtures.activeBankAccount(DomainFixtures.validIndividualClient());
        account.cancel();

        // Act / Assert
        assertThatThrownBy(account::block)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("canceled");
    }

    @Test
    void should_reconstruct_bank_account_with_all_fields() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        LocalDate openedOn = LocalDate.of(2024, 1, 15);

        // Act
        BankAccount account = BankAccount.reconstruct(
                "ACC-9999",
                AccountType.FIXED_TERM,
                holder,
                new BigDecimal("5000000"),
                CurrencyType.USD,
                openedOn,
                AccountStatus.BLOCKED
        );

        // Assert
        assertThat(account.getAccountNumber()).isEqualTo("ACC-9999");
        assertThat(account.getAccountType()).isEqualTo(AccountType.FIXED_TERM);
        assertThat(account.getHolder()).isSameAs(holder);
        assertThat(account.getCurrentBalance()).isEqualByComparingTo("5000000");
        assertThat(account.getCurrency()).isEqualTo(CurrencyType.USD);
        assertThat(account.getOpeningDate()).isEqualTo(openedOn);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.BLOCKED);
    }
}
