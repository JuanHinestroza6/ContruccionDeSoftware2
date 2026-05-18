package co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound payload to approve a loan currently in {@code UNDER_REVIEW}.
 *
 * <p>Only syntactic validation is performed here. The state-machine
 * invariant (loan must be {@code UNDER_REVIEW}) and authorization rules
 * are enforced by the corresponding use case.</p>
 */
public record ApproveLoanRequest(

        @NotNull(message = "approverId must not be null")
        UUID approverId,

        @NotNull(message = "approvedAmount must not be null")
        @DecimalMin(value = "0.01", inclusive = true, message = "approvedAmount must be >= 0.01")
        BigDecimal approvedAmount,

        @NotNull(message = "interestRate must not be null")
        @DecimalMin(value = "0.00", inclusive = true, message = "interestRate must be >= 0.00")
        BigDecimal interestRate,

        @NotNull(message = "approvalDate must not be null")
        @PastOrPresent(message = "approvalDate must not be a future date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate approvalDate
) {
}
