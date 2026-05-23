package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.ports.in.FindBankAccountUseCase;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application service — read-only queries for the {@link BankAccount} aggregate.
 *
 * <p>Queries are not audited.
 */
@Service
public class FindBankAccountService implements FindBankAccountUseCase {

    private final BankAccountRepositoryPort bankAccountRepositoryPort;

    public FindBankAccountService(BankAccountRepositoryPort bankAccountRepositoryPort) {
        this.bankAccountRepositoryPort = bankAccountRepositoryPort;
    }

    @Override
    public BankAccount findByAccountNumber(String accountNumber) {
        return bankAccountRepositoryPort.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BankAccount not found with account number: " + accountNumber));
    }

    @Override
    public List<BankAccount> findByClientId(UUID clientId) {
        return bankAccountRepositoryPort.findByClientId(clientId);
    }
}
