package co.edu.tdea.bank.domain.services;

import co.edu.tdea.bank.domain.ports.in.ApproveTransferUseCase;
import co.edu.tdea.bank.domain.ports.out.AuditLogPort;
import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.TransferRepositoryPort;
import co.edu.tdea.bank.domain.ports.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.domain.Exceptions.InsufficientFundsException;
import co.edu.tdea.bank.domain.Exceptions.InvalidStateTransitionException;
import co.edu.tdea.bank.domain.Exceptions.ResourceNotFoundException;
import co.edu.tdea.bank.domain.Exceptions.UnauthorizedOperationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Application service â€” orchestrates the approval and immediate execution of a
 * pending transfer.
 *
 * <p>Business rules enforced:
 * <ol>
 *   <li>The transfer must exist.</li>
 *   <li>The approver user must exist.</li>
 *   <li>The approver must hold {@code COMPANY_SUPERVISOR} or {@code BUSINESS_ADMIN}.</li>
 *   <li>Domain enforces {@code PENDING_APPROVAL â†’ EXECUTED} transition, account
 *       operability, and sufficient balance; any {@code IllegalStateException} is
 *       translated to the appropriate application-layer exception.</li>
 *   <li>Both accounts and the transfer are persisted after execution.</li>
 *   <li>Operation is audited with balance snapshot.</li>
 * </ol>
 */
@Service
public class ApproveTransferService implements ApproveTransferUseCase {

    private final TransferRepositoryPort transferRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final BankAccountRepositoryPort bankAccountRepositoryPort;
    private final AuditLogPort auditLogPort;

    public ApproveTransferService(TransferRepositoryPort transferRepositoryPort,
                                  UserRepositoryPort userRepositoryPort,
                                  BankAccountRepositoryPort bankAccountRepositoryPort,
                                  AuditLogPort auditLogPort) {
        this.transferRepositoryPort    = transferRepositoryPort;
        this.userRepositoryPort        = userRepositoryPort;
        this.bankAccountRepositoryPort = bankAccountRepositoryPort;
        this.auditLogPort              = auditLogPort;
    }

    @Override
    public Transfer approveTransfer(Long transferId,
                                    UUID approverId,
                                    LocalDateTime approvalDateTime) {

        // 1. Transfer must exist
        Transfer transfer = transferRepositoryPort.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", transferId));

        // 2. Approver must exist
        User approver = userRepositoryPort.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        // 3. Only COMPANY_SUPERVISOR or BUSINESS_ADMIN may approve transfers
        SystemRole role = approver.getSystemRole();
        if (role != SystemRole.COMPANY_SUPERVISOR && role != SystemRole.BUSINESS_ADMIN) {
            throw new UnauthorizedOperationException(
                    "User " + approverId + " with role " + role
                    + " is not authorized to approve transfers. "
                    + "Required role: COMPANY_SUPERVISOR or BUSINESS_ADMIN.");
        }

        // 4. Capture balances before execution for the audit trail
        BankAccount source      = transfer.getSourceAccount();
        BankAccount destination = transfer.getDestinationAccount();
        BigDecimal sourceBalanceBefore      = source.getCurrentBalance();
        BigDecimal destinationBalanceBefore = destination.getCurrentBalance();

        // 5. Apply domain transition â€” approve() sets approvedBy, approvalDateTime,
        //    then delegates to execute() which enforces PENDING_APPROVAL state,
        //    account operability and sufficient balance.
        try {
            transfer.approve(approver, approvalDateTime);
        } catch (IllegalStateException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("insufficient")) {
                throw new InsufficientFundsException(
                        source.getAccountNumber(),
                        sourceBalanceBefore,
                        transfer.getAmount());
            }
            throw new InvalidStateTransitionException(
                    "Transfer", transfer.getTransferStatus(), "approve");
        }

        // 6. Persist updated accounts
        bankAccountRepositoryPort.save(source);
        bankAccountRepositoryPort.save(destination);

        // 7. Persist updated transfer
        Transfer savedTransfer = transferRepositoryPort.save(transfer);

        // 8. Audit with balance snapshot (PDF requirement)
        auditLogPort.save(
                "Transfer",
                String.valueOf(savedTransfer.getTransferId()),
                "TRANSFER_APPROVED_EXECUTED",
                approverId.toString(),
                LocalDateTime.now(),
                "amount=" + savedTransfer.getAmount()
                + ", approverRole=" + role
                + ", sourceAccount=" + source.getAccountNumber()
                + ", sourceBalanceBefore=" + sourceBalanceBefore
                + ", sourceBalanceAfter=" + source.getCurrentBalance()
                + ", destinationAccount=" + destination.getAccountNumber()
                + ", destinationBalanceBefore=" + destinationBalanceBefore
                + ", destinationBalanceAfter=" + destination.getCurrentBalance()
                + ", approvalDateTime=" + approvalDateTime
        );

        return savedTransfer;
    }
}
