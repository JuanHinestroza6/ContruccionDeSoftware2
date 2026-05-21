package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;
import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.enums.LoanType;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoanServiceTest {

    @Mock private ClientRepositoryPort clientRepositoryPort;
    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private LoanRepositoryPort loanRepositoryPort;
    @Mock private AuditLogPort auditLogPort;

    @Test
    void should_request_loan_successfully_when_client_and_active_user_exist() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User activeUser = DomainFixtures.validUser();

        // savedLoan simulates what the persistence adapter returns after DB assigns the ID
        Loan savedLoan = Loan.reconstruct(42L, LoanType.PERSONAL, client,
                new BigDecimal("5000000"), 24,
                null, null, LoanStatus.UNDER_REVIEW, null, null, null);

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(activeUser));
        when(loanRepositoryPort.save(any(Loan.class))).thenReturn(savedLoan);

        RequestLoanService service = new RequestLoanService(
                clientRepositoryPort, userRepositoryPort, loanRepositoryPort, auditLogPort);

        // Act
        Loan result = service.requestLoan(clientId, LoanType.PERSONAL,
                new BigDecimal("5000000"), 24);

        // Assert
        assertThat(result).isSameAs(savedLoan);
        verify(loanRepositoryPort).save(any(Loan.class));
        verify(auditLogPort).save(eq("Loan"), eq("42"),
                eq("LOAN_REQUESTED"), eq(activeUser.getUserId().toString()),
                any(), anyString());
    }

    @Test
    void should_throw_ResourceNotFoundException_when_client_not_found() {
        // Arrange
        UUID missingClientId = UUID.randomUUID();
        when(clientRepositoryPort.findById(missingClientId)).thenReturn(Optional.empty());

        RequestLoanService service = new RequestLoanService(
                clientRepositoryPort, userRepositoryPort, loanRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.requestLoan(missingClientId, LoanType.PERSONAL,
                new BigDecimal("1000000"), 12))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client");
        verifyNoInteractions(loanRepositoryPort, auditLogPort);
    }

    @Test
    void should_throw_ResourceNotFoundException_when_linked_user_not_found() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.empty());

        RequestLoanService service = new RequestLoanService(
                clientRepositoryPort, userRepositoryPort, loanRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.requestLoan(clientId, LoanType.PERSONAL,
                new BigDecimal("1000000"), 12))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("System user linked to client");
        verifyNoInteractions(loanRepositoryPort, auditLogPort);
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_user_is_inactive() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User inactiveUser = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Alice Smith")
                .identificationId("U-INACTIVE")
                .email("alice@example.com")
                .phone("+57 300 0000000")
                .address("Cl 1 # 2-3")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .userStatus(UserStatus.INACTIVE)
                .build();

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(inactiveUser));

        RequestLoanService service = new RequestLoanService(
                clientRepositoryPort, userRepositoryPort, loanRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.requestLoan(clientId, LoanType.PERSONAL,
                new BigDecimal("1000000"), 12))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("INACTIVE");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }

    @Test
    void should_throw_UnauthorizedOperationException_when_user_is_blocked() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        UUID clientId = client.getClientId();
        User blockedUser = User.builder()
                .userId(UUID.randomUUID())
                .fullName("Bob Blocked")
                .identificationId("U-BLOCKED")
                .email("bob@example.com")
                .phone("+57 300 0000001")
                .address("Cl 1 # 2-3")
                .systemRole(SystemRole.COMMERCIAL_EMPLOYEE)
                .userStatus(UserStatus.BLOCKED)
                .build();

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findByRelatedClientId(clientId)).thenReturn(Optional.of(blockedUser));

        RequestLoanService service = new RequestLoanService(
                clientRepositoryPort, userRepositoryPort, loanRepositoryPort, auditLogPort);

        // Act + Assert
        assertThatThrownBy(() -> service.requestLoan(clientId, LoanType.PERSONAL,
                new BigDecimal("1000000"), 12))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("BLOCKED");
        verify(loanRepositoryPort, never()).save(any());
        verify(auditLogPort, never()).save(anyString(), anyString(), anyString(),
                anyString(), any(), any());
    }
}
