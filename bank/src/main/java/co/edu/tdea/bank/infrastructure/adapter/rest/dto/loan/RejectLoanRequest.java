package co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Inbound payload to reject a loan currently in {@code UNDER_REVIEW}.
 *
 * <p>Only syntactic validation is performed here. The state-machine
 * invariant and authorization rules are enforced by the corresponding
 * use case.</p>
 */
public record RejectLoanRequest(

        @NotNull(message = "approverId must not be null")
        UUID approverId
) {
}
