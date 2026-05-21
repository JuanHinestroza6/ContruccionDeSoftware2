package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;
import co.edu.tdea.bank.testfixtures.DomainFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindLoanServiceTest {

    @Mock private LoanRepositoryPort loanRepositoryPort;

    @Test
    void should_return_loan_when_found_by_id() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        when(loanRepositoryPort.findById(loan.getLoanId())).thenReturn(Optional.of(loan));

        FindLoanService service = new FindLoanService(loanRepositoryPort);

        // Act
        Loan result = service.findById(loan.getLoanId());

        // Assert
        assertThat(result).isEqualTo(loan);
    }

    @Test
    void should_throw_ResourceNotFoundException_when_loan_id_not_found() {
        // Arrange
        Long missing = 9999L;
        when(loanRepositoryPort.findById(missing)).thenReturn(Optional.empty());

        FindLoanService service = new FindLoanService(loanRepositoryPort);

        // Act + Assert
        assertThatThrownBy(() -> service.findById(missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan");
    }

    @Test
    void should_return_loans_for_client_when_queried_by_client_id() {
        // Arrange
        IndividualClient client = DomainFixtures.validIndividualClient();
        Loan loan = DomainFixtures.validLoanUnderReview(client);
        UUID clientId = client.getClientId();
        when(loanRepositoryPort.findByClientId(clientId)).thenReturn(List.of(loan));

        FindLoanService service = new FindLoanService(loanRepositoryPort);

        // Act
        List<Loan> result = service.findByClientId(clientId);

        // Assert
        assertThat(result).containsExactly(loan);
    }
}
