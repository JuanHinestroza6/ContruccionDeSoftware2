package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.domain.ports.in.FindLoanUseCase;
import co.edu.tdea.bank.domain.ports.out.LoanRepositoryPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application service — read-only queries for the {@link Loan} aggregate.
 *
 * <p>Queries are not audited.
 */
@Service
public class FindLoanService implements FindLoanUseCase {

    private final LoanRepositoryPort loanRepositoryPort;

    public FindLoanService(LoanRepositoryPort loanRepositoryPort) {
        this.loanRepositoryPort = loanRepositoryPort;
    }

    @Override
    public Loan findById(Long loanId) {
        return loanRepositoryPort.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Loan not found with ID: " + loanId));
    }

    @Override
    public List<Loan> findByClientId(UUID clientId) {
        return loanRepositoryPort.findByClientId(clientId);
    }
}
