package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.domain.ports.in.ApproveTransferUseCase;
import co.edu.tdea.bank.domain.ports.in.CreateTransferUseCase;
import co.edu.tdea.bank.domain.ports.in.FindTransferUseCase;
import co.edu.tdea.bank.domain.ports.in.RejectTransferUseCase;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.ApproveTransferRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.CreateTransferRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.RejectTransferRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer.TransferResponse;
import co.edu.tdea.bank.infrastructure.adapter.rest.mapper.TransferDtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the {@code Transfer} aggregate.
 *
 * <p>Pure delegating layer — every endpoint validates input, hands the call
 * off to the corresponding input port, and serializes the domain result via
 * {@link TransferDtoMapper}. No business decisions live in this class.</p>
 *
 * <p>TODO: secure these endpoints once the security adapter lands
 * (e.g. {@code @PreAuthorize("hasAnyRole('COMPANY_SUPERVISOR','BUSINESS_ADMIN')")}
 * on the approve and reject operations).</p>
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final CreateTransferUseCase createTransferUseCase;
    private final ApproveTransferUseCase approveTransferUseCase;
    private final RejectTransferUseCase rejectTransferUseCase;
    private final FindTransferUseCase findTransferUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase,
                              ApproveTransferUseCase approveTransferUseCase,
                              RejectTransferUseCase rejectTransferUseCase,
                              FindTransferUseCase findTransferUseCase) {
        this.createTransferUseCase  = createTransferUseCase;
        this.approveTransferUseCase = approveTransferUseCase;
        this.rejectTransferUseCase  = rejectTransferUseCase;
        this.findTransferUseCase    = findTransferUseCase;
    }

    /**
     * Initiates a new fund transfer between two bank accounts. The resulting
     * transfer is returned either in {@code PENDING_APPROVAL} or {@code EXECUTED}
     * status, depending on whether the amount meets the supplied approval
     * threshold.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @Valid @RequestBody CreateTransferRequest request) {

        Transfer created = createTransferUseCase.createTransfer(
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.createdByUserId(),
                request.creationDateTime(),
                request.approvalThreshold()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransferDtoMapper.toResponse(created));
    }

    /**
     * Retrieves a transfer by its technical identifier.
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> findById(@PathVariable Long transferId) {
        Transfer transfer = findTransferUseCase.findById(transferId);
        return ResponseEntity.ok(TransferDtoMapper.toResponse(transfer));
    }

    /**
     * Lists every transfer whose source matches the given account number.
     * Returns an empty list (never 404) when the account has no transfers.
     */
    @GetMapping("/by-account/{accountNumber}")
    public ResponseEntity<List<TransferResponse>> findBySourceAccountNumber(
            @PathVariable String accountNumber) {

        List<TransferResponse> transfers = findTransferUseCase
                .findBySourceAccountNumber(accountNumber)
                .stream()
                .map(TransferDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(transfers);
    }

    /**
     * Lists every transfer initiated by the given user. Returns an empty list
     * (never 404) when the user has no transfers.
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<TransferResponse>> findByCreatedByUserId(
            @PathVariable UUID userId) {

        List<TransferResponse> transfers = findTransferUseCase
                .findByCreatedByUserId(userId)
                .stream()
                .map(TransferDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(transfers);
    }

    /**
     * Approves a transfer currently in {@code PENDING_APPROVAL} and executes
     * the fund movement.
     */
    @PatchMapping("/{transferId}/approve")
    public ResponseEntity<TransferResponse> approveTransfer(
            @PathVariable Long transferId,
            @Valid @RequestBody ApproveTransferRequest request) {

        Transfer approved = approveTransferUseCase.approveTransfer(
                transferId,
                request.approverId(),
                request.approvalDateTime()
        );
        return ResponseEntity.ok(TransferDtoMapper.toResponse(approved));
    }

    /**
     * Rejects a transfer currently in {@code PENDING_APPROVAL} without moving
     * any funds.
     */
    @PatchMapping("/{transferId}/reject")
    public ResponseEntity<TransferResponse> rejectTransfer(
            @PathVariable Long transferId,
            @Valid @RequestBody RejectTransferRequest request) {

        Transfer rejected = rejectTransferUseCase.rejectTransfer(
                transferId,
                request.approverId()
        );
        return ResponseEntity.ok(TransferDtoMapper.toResponse(rejected));
    }
}
