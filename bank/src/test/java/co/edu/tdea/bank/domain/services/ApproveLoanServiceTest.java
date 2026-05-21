package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;
import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
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
class ApproveLoanServiceTest {

    @Mock private LoanRepositoryPort loanRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    private static User analystUser(UUID userId) {
        return User.builder()
                .userId(userId)
                .fullName("Anna Analyst")
                .identificationId("U-" + userId.toString().substring(0, 8))
                .email("anna@example.com")
                .phone("+57 300 0000000")
                .address("HQ, Medellin")
                .systemRole(SystemRole.INTERNAL_ANALYST)
                .userStatus(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void should_approve_loan_successfully_when_loan_under_review_and_user_is_analyst() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID approverId = UUID.randomUUID();
        User analyst = analystUser(approverId);

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(approverId)).thenReturn(Optional.of(analyst));
        when(loanRepositoryPort.save(loan)).thenReturn(loan);

        ApproveLoanService service = new ApproveLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act
        Loan result = service.approveLoan(loan.getLoanId(), approverId,
                new BigDecimal("5000000"), new BigDecimal("0.15"),
                LocalDate.of(2026, 1, 15));

        // Assert
        assertThat(result.getLoanStatus()).isEqualTo(LoanStatus.APPROVED);
        verify(loanRepositoryPort).save(loan);
        verify(auditLogPort).save(eq("Loan"), eq(String.valueOf(loan.getLoanId())),
                eq("LOAN_APPROVED"), eq(approverId.toString()), any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_loan_not_found() {
        // Arrange
        Long missingLoanId = 9999L;
        when(loanRepositoryPort.findById(missingLoanId)).thenReturn(Optional.empty());

        ApproveLoanService service = new ApproveLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.approveLoan(missingLoanId, UUID.randomUUID(),
                new BigDecimal("1000"), new BigDecimal("0.10"), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_approver_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID approverId = UUID.randomUUID();

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(approverId)).thenReturn(Optional.empty());

        ApproveLoanService service = new ApproveLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.approveLoan(loan.getLoanId(), approverId,
                new BigDecimal("1000"), new BigDecimal("0.10"), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_approver_is_not_analyst() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID approverId = UUID.randomUUID();
        User nonAnalyst = User.builder()
                .userId(approverId)
                .fullName("Teller Tim")
                .identificationId("U-TELLER")
                .email("tim@example.com")
                .phone("+57 300 0000001")
                .address("HQ, Medellin")
                .systemRole(SystemRole.TELLER_EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(approverId)).thenReturn(Optional.of(nonAnalyst));

        ApproveLoanService service = new ApproveLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.approveLoan(loan.getLoanId(), approverId,
                new BigDecimal("1000"), new BigDecimal("0.10"), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("INTERNAL_ANALYST");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
