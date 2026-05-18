package co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbound payload to approve a transfer currently in {@code PENDING_APPROVAL}.
 *
 * <p>Only syntactic validation is performed here. The state-machine invariant
 * and authorization rules are enforced by the corresponding use case.</p>
 */
public record ApproveTransferRequest(

        @NotNull(message = "approverId must not be null")
        UUID approverId,

        @NotNull(message = "approvalDateTime must not be null")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime approvalDateTime
) {
}
