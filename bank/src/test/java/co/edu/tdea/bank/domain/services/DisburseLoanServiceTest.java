package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;
import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisburseLoanServiceTest {

    @Mock private LoanRepositoryPort loanRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private BankAccountRepositoryPort bankAccountRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static User analyst(UUID id) {
        return User.builder()
                .userId(id)
                .fullName("Anna Analyst")
                .identificationId("U-" + id.toString().substring(0, 8))
                .email("anna@example.com")
                .phone("+57 300 0000000")
                .address("HQ, Medellin")
                .systemRole(SystemRole.INTERNAL_ANALYST)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    private static Loan approvedLoan(IndividualClient client) {
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        loan.approve(new BigDecimal("5000000"), new BigDecimal("0.15"),
                LocalDate.of(2026, 1, 15));
        return loan;
    }

    @Test
    void should_disburse_loan_successfully_when_loan_approved_and_account_active() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = approvedLoan(client);
        BankAccount destination = DomainFixtures.activeBankAccount(client);
        BigDecimal balanceBefore = destination.getCurrentBalance();
        UUID analystId = UUID.randomUUID();
        User analyst = analyst(analystId);

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(analystId)).thenReturn(Optional.of(analyst));
        when(bankAccountRepositoryPort.findByAccountNumber(destination.getAccountNumber()))
                .thenReturn(Optional.of(destination));
        when(loanRepositoryPort.save(loan)).thenReturn(loan);

        DisburseLoanService service = new DisburseLoanService(
                loanRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act
        Loan result = service.disburseLoan(loan.getLoanId(), destination.getAccountNumber(),
                LocalDate.of(2026, 1, 20), analystId);

        // Assert
        assertThat(result.getLoanStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(destination.getCurrentBalance())
                .isEqualByComparingTo(balanceBefore.add(loan.getApprovedAmount()));
        verify(loanRepositoryPort).save(loan);
        verify(bankAccountRepositoryPort).save(destination);
        verify(auditLogPort).save(eq("Loan"), eq(String.valueOf(loan.getLoanId())),
                eq("LOAN_DISBURSED"), eq(analystId.toString()), any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_loan_not_found() {
        // Arrange
        when(loanRepositoryPort.findById(9999L)).thenReturn(Optional.empty());

        DisburseLoanService service = new DisburseLoanService(
                loanRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.disburseLoan(9999L, "ACC-001",
                LocalDate.of(2026, 1, 20), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
        verify(loanRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_analyst_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = approvedLoan(client);
        UUID analystId = UUID.randomUUID();

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(analystId)).thenReturn(Optional.empty());

        DisburseLoanService service = new DisburseLoanService(
                loanRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.disburseLoan(loan.getLoanId(), "ACC-001",
                LocalDate.of(2026, 1, 20), analystId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(loanRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_destination_account_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = approvedLoan(client);
        UUID analystId = UUID.randomUUID();
        User analyst = analyst(analystId);
        String missingAccountNumber = "ACC-MISSING";

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(analystId)).thenReturn(Optional.of(analyst));
        when(bankAccountRepositoryPort.findByAccountNumber(missingAccountNumber))
                .thenReturn(Optional.empty());

        DisburseLoanService service = new DisburseLoanService(
                loanRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.disburseLoan(loan.getLoanId(), missingAccountNumber,
                LocalDate.of(2026, 1, 20), analystId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("BankAccount");
        verify(loanRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_disbursement_by_non_analyst() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = approvedLoan(client);
        UUID userId = UUID.randomUUID();
        User notAnalyst = User.builder()
                .userId(userId)
                .fullName("Teller Tim")
                .identificationId("U-TELLER")
                .email("tim@example.com")
                .phone("+57 300 0000001")
                .address("HQ, Medellin")
                .systemRole(SystemRole.TELLER_EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(notAnalyst));

        DisburseLoanService service = new DisburseLoanService(
                loanRepositoryPort, userRepositoryPort, bankAccountRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.disburseLoan(loan.getLoanId(), "ACC-001",
                LocalDate.of(2026, 1, 20), userId))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("INTERNAL_ANALYST");
        verify(loanRepositoryPort, never()).save(any());
        verify(bankAccountRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
