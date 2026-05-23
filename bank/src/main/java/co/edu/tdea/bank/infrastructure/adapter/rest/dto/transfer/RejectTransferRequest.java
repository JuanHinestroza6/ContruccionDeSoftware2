package co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Inbound payload to reject a transfer currently in {@code PENDING_APPROVAL}.
 *
 * <p>Only syntactic validation is performed here. The state-machine invariant
 * and authorization rules are enforced by the corresponding use case.</p>
 */
public record RejectTransferRequest(

        @NotNull(message = "approverId must not be null")
        UUID approverId
) {
}
