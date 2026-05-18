package co.edu.tdea.bank.infrastructure.adapter.rest.dto.account;

import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound payload to open a new {@code BankAccount} for an existing client.
 *
 * <p>All fields are mandatory. Domain-level invariants (e.g. uniqueness of
 * {@code accountNumber}, existence of the {@code clientId}) are enforced by
 * the corresponding use case — this DTO only performs syntactic validation.</p>
 */
public record OpenBankAccountRequest(

        @NotNull(message = "clientId must not be null")
        UUID clientId,

        @NotBlank(message = "accountNumber must not be blank")
        String accountNumber,

        @NotNull(message = "accountType must not be null")
        AccountType accountType,

        @NotNull(message = "initialBalance must not be null")
        @DecimalMin(value = "0.00", message = "initialBalance must be >= 0.00")
        BigDecimal initialBalance,

        @NotNull(message = "currency must not be null")
        CurrencyType currency
) {
}
