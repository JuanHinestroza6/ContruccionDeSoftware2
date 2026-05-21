package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 10, 0);

    // -------------------------------------------------------------------------
    // Creation
    // -------------------------------------------------------------------------

    @Test
    void should_create_transfer_when_all_fields_valid() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("500000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        User createdBy = DomainFixtures.validUser();

        // Act
        Transfer transfer = Transfer.create(
                source,
                destination,
                new BigDecimal("50000"),
                BASE_TIME,
                createdBy
        );

        // Assert
        assertThat(transfer.getTransferId()).isNull();
        assertThat(transfer.getSourceAccount()).isSameAs(source);
        assertThat(transfer.getDestinationAccount()).isSameAs(destination);
        assertThat(transfer.getAmount()).isEqualByComparingTo("50000");
        assertThat(transfer.getCreationDateTime()).isEqualTo(BASE_TIME);
        assertThat(transfer.getCreatedBy()).isSameAs(createdBy);
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
        assertThat(transfer.getApprovalDateTime()).isNull();
        assertThat(transfer.getApprovedBy()).isNull();
    }

    @Test
    void should_throw_when_amount_is_zero_or_negative() {
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder);
        BankAccount destination = DomainFixtures.activeBankAccount(holder);
        User user = DomainFixtures.validUser();

        assertThatThrownBy(() -> Transfer.create(source, destination, BigDecimal.ZERO, BASE_TIME, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> Transfer.create(source, destination, new BigDecimal("-1"), BASE_TIME, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void should_throw_when_source_equals_destination() {
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder);
        User user = DomainFixtures.validUser();

        assertThatThrownBy(() -> Transfer.create(source, source, new BigDecimal("100"), BASE_TIME, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    void should_throw_when_createdBy_is_null() {
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder);
        BankAccount destination = DomainFixtures.activeBankAccount(holder);

        assertThatThrownBy(() -> Transfer.create(source, destination, new BigDecimal("100"), BASE_TIME, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("createdBy");
    }

    // -------------------------------------------------------------------------
    // execute()
    // -------------------------------------------------------------------------

    @Test
    void should_execute_transfer_and_move_funds() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, new BigDecimal("200"));
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("300"), DomainFixtures.validUser());

        // Act
        transfer.execute();

        // Assert
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.EXECUTED);
        assertThat(source.getCurrentBalance()).isEqualByComparingTo("700");
        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("500");
    }

    @Test
    void should_throw_when_executing_already_executed_transfer() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());
        transfer.execute();

        // Act / Assert
        assertThatThrownBy(transfer::execute)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXECUTED");
    }

    @Test
    void should_throw_when_source_account_has_insufficient_balance() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("50"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());

        // Act / Assert
        assertThatThrownBy(transfer::execute)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");

        // Source must remain unchanged
        assertThat(source.getCurrentBalance()).isEqualByComparingTo("50");
        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("0");
        // Transfer must not be marked as EXECUTED
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
    }

    @Test
    void should_throw_when_executing_with_blocked_source_account() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());
        source.block();

        // Act / Assert
        assertThatThrownBy(transfer::execute)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source");
    }

    // -------------------------------------------------------------------------
    // approve()
    // -------------------------------------------------------------------------

    @Test
    void should_approve_and_execute_transfer() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        User approver = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("250"), DomainFixtures.validUser());
        LocalDateTime approveAt = BASE_TIME.plusMinutes(5);

        // Act
        transfer.approve(approver, approveAt);

        // Assert
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.EXECUTED);
        assertThat(transfer.getApprovedBy()).isSameAs(approver);
        assertThat(transfer.getApprovalDateTime()).isEqualTo(approveAt);
        assertThat(source.getCurrentBalance()).isEqualByComparingTo("750");
        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("250");
    }

    @Test
    void should_throw_when_approving_non_pending_transfer() {
        // Arrange — already REJECTED
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        User approver = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());
        transfer.reject(approver);

        // Act / Assert
        assertThatThrownBy(() -> transfer.approve(approver, BASE_TIME.plusMinutes(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approve");
    }

    // -------------------------------------------------------------------------
    // reject()
    // -------------------------------------------------------------------------

    @Test
    void should_reject_transfer() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        User approver = DomainFixtures.validUser();
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());

        // Act
        transfer.reject(approver);

        // Assert
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.REJECTED);
        assertThat(transfer.getApprovedBy()).isSameAs(approver);
        // No funds moved
        assertThat(source.getCurrentBalance()).isEqualByComparingTo("1000");
        assertThat(destination.getCurrentBalance()).isEqualByComparingTo("0");
    }

    @Test
    void should_throw_when_rejecting_non_pending_transfer() {
        // Arrange — already EXPIRED
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());
        transfer.expire();

        // Act / Assert
        assertThatThrownBy(() -> transfer.reject(DomainFixtures.validUser()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reject");
    }

    // -------------------------------------------------------------------------
    // expire()
    // -------------------------------------------------------------------------

    @Test
    void should_expire_transfer() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());

        // Act
        transfer.expire();

        // Assert
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.EXPIRED);
    }

    @Test
    void should_throw_when_expiring_non_pending_transfer() {
        // Arrange — already EXECUTED
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), DomainFixtures.validUser());
        transfer.execute();

        // Act / Assert
        assertThatThrownBy(transfer::expire)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expire");
    }

    // -------------------------------------------------------------------------
    // isExpired() — deterministic time, no Clock needed (method takes 'now' as input)
    // -------------------------------------------------------------------------

    @Test
    void should_return_true_when_transfer_is_expired() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), BASE_TIME, DomainFixtures.validUser());
        Duration window = Duration.ofMinutes(60);
        LocalDateTime later = BASE_TIME.plusMinutes(70);

        // Act
        boolean expired = transfer.isExpired(later, window);

        // Assert
        assertThat(expired).isTrue();
    }

    @Test
    void should_return_false_when_transfer_is_not_expired() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), BASE_TIME, DomainFixtures.validUser());
        Duration window = Duration.ofMinutes(60);
        LocalDateTime earlier = BASE_TIME.plusMinutes(30);

        // Act
        boolean expired = transfer.isExpired(earlier, window);

        // Assert
        assertThat(expired).isFalse();
    }

    @Test
    void should_return_false_when_transfer_is_in_terminal_state() {
        // Arrange — EXECUTED transfer cannot expire by definition
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("100"), BASE_TIME, DomainFixtures.validUser());
        transfer.execute();

        // Act
        boolean expired = transfer.isExpired(BASE_TIME.plusDays(365), Duration.ofMinutes(60));

        // Assert
        assertThat(expired).isFalse();
    }

    // -------------------------------------------------------------------------
    // requiresApproval()
    // -------------------------------------------------------------------------

    @Test
    void should_require_approval_when_amount_exceeds_threshold() {
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        Transfer transfer = DomainFixtures.validPendingTransfer(
                source, destination, new BigDecimal("500000"), DomainFixtures.validUser());

        assertThat(transfer.requiresApproval(new BigDecimal("100000"))).isTrue();
        assertThat(transfer.requiresApproval(new BigDecimal("500000"))).isFalse(); // strict >
    }

    // -------------------------------------------------------------------------
    // reconstruct()
    // -------------------------------------------------------------------------

    @Test
    void should_reconstruct_executed_transfer_with_all_fields() {
        // Arrange
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, new BigDecimal("100"));
        User creator = DomainFixtures.validUser();
        User approver = DomainFixtures.validUser();
        LocalDateTime createdAt = BASE_TIME;
        LocalDateTime approvedAt = BASE_TIME.plusMinutes(10);

        // Act
        Transfer transfer = Transfer.reconstruct(
                500L,
                source,
                destination,
                new BigDecimal("250"),
                createdAt,
                creator,
                approvedAt,
                TransferStatus.EXECUTED,
                approver
        );

        // Assert
        assertThat(transfer.getTransferId()).isEqualTo(500L);
        assertThat(transfer.getTransferStatus()).isEqualTo(TransferStatus.EXECUTED);
        assertThat(transfer.getCreatedBy()).isSameAs(creator);
        assertThat(transfer.getApprovedBy()).isSameAs(approver);
        assertThat(transfer.getCreationDateTime()).isEqualTo(createdAt);
        assertThat(transfer.getApprovalDateTime()).isEqualTo(approvedAt);
    }

    @Test
    void should_throw_when_reconstructing_executed_transfer_without_approver() {
        Client holder = DomainFixtures.validIndividualClient();
        BankAccount source = DomainFixtures.activeBankAccount(holder, new BigDecimal("1000"));
        BankAccount destination = DomainFixtures.activeBankAccount(holder, BigDecimal.ZERO);
        User creator = DomainFixtures.validUser();

        assertThatThrownBy(() -> Transfer.reconstruct(
                600L,
                source,
                destination,
                new BigDecimal("100"),
                BASE_TIME,
                creator,
                BASE_TIME.plusMinutes(5),
                TransferStatus.EXECUTED,
                null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("approvedBy");
    }
}
