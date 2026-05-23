package co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound payload to disburse an approved loan into a destination account.
 *
 * <p>Only syntactic validation is performed here. The state-machine
 * invariant (loan must be {@code APPROVED}, destination account must be
 * {@code ACTIVE}) and authorization rules are enforced by the
 * corresponding use case.</p>
 */
public record DisburseLoanRequest(

        @NotBlank(message = "destinationAccountNumber must not be blank")
        String destinationAccountNumber,

        @NotNull(message = "disbursementDate must not be null")
        @PastOrPresent(message = "disbursementDate must not be a future date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate disbursementDate,

        @NotNull(message = "analystUserId must not be null")
        UUID analystUserId
) {
}
