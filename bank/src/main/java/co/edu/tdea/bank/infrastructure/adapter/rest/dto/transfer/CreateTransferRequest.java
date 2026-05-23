package co.edu.tdea.bank.infrastructure.adapter.rest.dto.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbound payload to initiate a new fund transfer between two bank accounts.
 *
 * <p>Only syntactic validation is performed here. Domain-level invariants
 * (account existence, operability, sufficient balance, source != destination)
 * are enforced by the corresponding use case.</p>
 */
public record CreateTransferRequest(

        @NotBlank(message = "sourceAccountNumber must not be blank")
        String sourceAccountNumber,

        @NotBlank(message = "destinationAccountNumber must not be blank")
        String destinationAccountNumber,

        @NotNull(message = "amount must not be null")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be >= 0.01")
        BigDecimal amount,

        @NotNull(message = "createdByUserId must not be null")
        UUID createdByUserId,

        @NotNull(message = "creationDateTime must not be null")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime creationDateTime,

        @NotNull(message = "approvalThreshold must not be null")
        @DecimalMin(value = "0.01", inclusive = true, message = "approvalThreshold must be >= 0.01")
        BigDecimal approvalThreshold
) {
}
