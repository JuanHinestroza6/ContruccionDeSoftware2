package co.edu.tdea.bank.infrastructure.adapter.rest.dto.account;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound representation of a {@code BankAccount} aggregate.
 *
 * <p>Enum-valued attributes ({@code accountType}, {@code currency},
 * {@code accountStatus}) are serialized as their {@link Enum#name()} string
 * to decouple wire format from the domain enum classes.</p>
 */
public record BankAccountResponse(

        String accountNumber,
        String accountType,
        UUID holderClientId,
        BigDecimal currentBalance,
        String currency,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate openingDate,

        String accountStatus
) {
}
