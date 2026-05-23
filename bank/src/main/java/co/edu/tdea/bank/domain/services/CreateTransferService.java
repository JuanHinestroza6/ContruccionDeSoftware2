package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.in.CreateTransferUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.Exceptions.InsufficientFundsException;
import co.edu.tdea.bank.domain.Exceptions.InvalidStateTransitionException;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service â€” orchestrates the creation of a fund transfer.
 *
 * <p>Two execution paths depending on the approval threshold:
 * <ul>
 *   <li><b>Requires approval</b> ({@code amount >= threshold}): transfer is saved in
 *       {@code PENDING_APPROVAL}; no funds are moved yet.</li>
 *   <li><b>No approval needed</b> ({@code amount < threshold}): transfer is executed
 *       immediately; both accounts are updated and persisted.</li>
 * </ul>
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The creator user must exist.</li>
 *   <li>The source account must exist.</li>
 *   <li>The destination account must exist.</li>
 *   <li>All domain invariants (account operability, sufficient balance) are enforced
 *       by the domain model; exceptions are translated to application-layer types.</li>
 * </ol>
 */
@Service
public class CreateTransferService implements CreateTransferUseCase {

    private final TransferRepositoryPort transferRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final BankAccountRepositoryPort bankAccountRepositoryPort;
    private final AuditLogPort auditLogPort;

    public CreateTransferService(TransferRepositoryPort transferRepositoryPort,
                                 UserRepositoryPort userRepositoryPort,
                                 BankAccountRepositoryPort bankAccountRepositoryPort,
                                 AuditLogPort auditLogPort) {
        this.transferRepositoryPort    = transferRepositoryPort;
        this.userRepositoryPort        = userRepositoryPort;
        this.bankAccountRepositoryPort = bankAccountRepositoryPort;
        this.auditLogPort              = auditLogPort;
    }

    @Override
    public Transfer createTransfer(String sourceAccountNumber,
                                   String destinationAccountNumber,
                                   BigDecimal amount,
                                   UUID createdByUserId,
                                   LocalDateTime creationDateTime,
                                   BigDecimal approvalThreshold) {

        // 1. Creator user must exist
        User creator = userRepositoryPort.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", createdByUserId));

        // 2. Source account must exist
        BankAccount source = bankAccountRepositoryPort
                .findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BankAccount", sourceAccountNumber));

        // 3. Destination account must exist
        BankAccount destination = bankAccountRepositoryPort
                .findByAccountNumber(destinationAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BankAccount", destinationAccountNumber));

        // 4. Build the transfer aggregate â€” ID is assigned by the persistence layer
        Transfer transfer = Transfer.create(source, destination,
                                            amount, creationDateTime, creator);

        // 5. Route by approval requirement
        if (transfer.requiresApproval(approvalThreshold)) {
            return handlePendingApproval(transfer, createdByUserId, approvalThreshold);
        } else {
            return handleDirectExecution(transfer, source, destination, createdByUserId);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Transfer handlePendingApproval(Transfer transfer,
                                           UUID createdByUserId,
                                           BigDecimal approvalThreshold) {
        // Transfer stays in PENDING_APPROVAL â€” no fund movement
        Transfer savedTransfer = transferRepositoryPort.save(transfer);

        auditLogPort.save(
                "Transfer",
                String.valueOf(savedTransfer.getTransferId()),
                "TRANSFER_PENDING_APPROVAL",
                createdByUserId.toString(),
                LocalDateTime.now(),
                "amount=" + savedTransfer.getAmount()
                + ", approvalThreshold=" + approvalThreshold
                + ", sourceAccount=" + savedTransfer.getSourceAccount().getAccountNumber()
                + ", destinationAccount=" + savedTransfer.getDestinationAccount().getAccountNumber()
        );

        return savedTransfer;
    }

    private Transfer handleDirectExecution(Transfer transfer,
                                           BankAccount source,
                                           BankAccount destination,
                                           UUID createdByUserId) {
        // Capture balances before execution for the audit trail
        BigDecimal sourceBalanceBefore      = source.getCurrentBalance();
        BigDecimal destinationBalanceBefore = destination.getCurrentBalance();

        // Execute â€” domain enforces account operability and sufficient balance.
        // IllegalStateException covers both invalid state and insufficient funds;
        // we inspect the message to route to the most specific exception.
        try {
            transfer.execute();
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("insufficient")) {
                throw new InsufficientFundsException(
                        source.getAccountNumber(),
                        sourceBalanceBefore,
                        transfer.getAmount());
            }
            throw new InvalidStateTransitionException(
                    "Transfer", transfer.getTransferStatus(), "execute");
        }

        // Persist updated accounts
        bankAccountRepositoryPort.save(source);
        bankAccountRepositoryPort.save(destination);

        // Persist executed transfer
        Transfer savedTransfer = transferRepositoryPort.save(transfer);

        // Audit with balance snapshot (PDF requirement)
        auditLogPort.save(
                "Transfer",
                String.valueOf(savedTransfer.getTransferId()),
                "TRANSFER_EXECUTED",
                createdByUserId.toString(),
                LocalDateTime.now(),
                "amount=" + savedTransfer.getAmount()
                + ", sourceAccount=" + source.getAccountNumber()
                + ", sourceBalanceBefore=" + sourceBalanceBefore
                + ", sourceBalanceAfter=" + source.getCurrentBalance()
                + ", destinationAccount=" + destination.getAccountNumber()
                + ", destinationBalanceBefore=" + destinationBalanceBefore
                + ", destinationBalanceAfter=" + destination.getCurrentBalance()
        );

        return savedTransfer;
    }
}
