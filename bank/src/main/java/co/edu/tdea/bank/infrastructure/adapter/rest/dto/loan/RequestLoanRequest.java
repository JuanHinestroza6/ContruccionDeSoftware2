package co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan;

import co.edu.tdea.bank.domain.enums.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound payload to submit a new loan request on behalf of a client.
 *
 * <p>Only syntactic validation is performed here. Domain-level invariants
 * (e.g. existence of the {@code clientId}, eligibility rules) are enforced
 * by the corresponding use case.</p>
 */
public record RequestLoanRequest(

        @NotNull(message = "clientId must not be null")
        UUID clientId,

        @NotNull(message = "loanType must not be null")
        LoanType loanType,

        @NotNull(message = "requestedAmount must not be null")
        @DecimalMin(value = "0.01", inclusive = true, message = "requestedAmount must be >= 0.01")
        BigDecimal requestedAmount,

        @NotNull(message = "termInMonths must not be null")
        @Min(value = 1, message = "termInMonths must be >= 1")
        Integer termInMonths
) {
}
