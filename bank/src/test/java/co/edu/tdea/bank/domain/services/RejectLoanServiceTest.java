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
class RejectLoanServiceTest {

    @Mock private LoanRepositoryPort loanRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
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

    @Test
    void should_reject_loan_successfully_when_loan_under_review_and_user_is_analyst() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID rejectorId = UUID.randomUUID();
        User rejector = analyst(rejectorId);

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(rejectorId)).thenReturn(Optional.of(rejector));
        when(loanRepositoryPort.save(loan)).thenReturn(loan);

        RejectLoanService service = new RejectLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act
        Loan result = service.rejectLoan(loan.getLoanId(), rejectorId);

        // Assert
        assertThat(result.getLoanStatus()).isEqualTo(LoanStatus.REJECTED);
        verify(loanRepositoryPort).save(loan);
        verify(auditLogPort).save(eq("Loan"), eq(String.valueOf(loan.getLoanId())),
                eq("LOAN_REJECTED"), eq(rejectorId.toString()), any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_loan_not_found() {
        // Arrange
        Long missingLoanId = 9999L;
        when(loanRepositoryPort.findById(missingLoanId)).thenReturn(Optional.empty());

        RejectLoanService service = new RejectLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.rejectLoan(missingLoanId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_rejector_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID rejectorId = UUID.randomUUID();

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(rejectorId)).thenReturn(Optional.empty());

        RejectLoanService service = new RejectLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.rejectLoan(loan.getLoanId(), rejectorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_rejector_is_not_analyst() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID rejectorId = UUID.randomUUID();
        User notAnalyst = User.builder()
                .userId(rejectorId)
                .fullName("Carl Commercial")
                .identificationId("U-COMM")
                .email("carl@example.com")
                .phone("+57 300 0000003")
                .address("HQ, Medellin")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));
        when(userRepositoryPort.findById(rejectorId)).thenReturn(Optional.of(notAnalyst));

        RejectLoanService service = new RejectLoanService(
                loanRepositoryPort, userRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.rejectLoan(loan.getLoanId(), rejectorId))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("INTERNAL_ANALYST");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
