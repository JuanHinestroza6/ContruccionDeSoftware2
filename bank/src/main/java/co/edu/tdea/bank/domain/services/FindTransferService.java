package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.ports.in.FindTransferUseCase;
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application service — read-only queries for the {@link Transfer} aggregate.
 *
 * <p>Queries are not audited.
 */
@Service
public class FindTransferService implements FindTransferUseCase {

    private final TransferRepositoryPort transferRepositoryPort;

    public FindTransferService(TransferRepositoryPort transferRepositoryPort) {
        this.transferRepositoryPort = transferRepositoryPort;
    }

    @Override
    public Transfer findById(Long transferId) {
        return transferRepositoryPort.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transfer not found with ID: " + transferId));
    }

    @Override
    public List<Transfer> findBySourceAccountNumber(String accountNumber) {
        return transferRepositoryPort.findBySourceAccountNumber(accountNumber);
    }

    @Override
    public List<Transfer> findByCreatedByUserId(UUID userId) {
        return transferRepositoryPort.findByCreatedByUserId(userId);
    }
}
