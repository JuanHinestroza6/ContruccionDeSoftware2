package co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound representation of a {@code Loan} aggregate.
 *
 * <p>Enum-valued attributes ({@code loanType}, {@code loanStatus}) are
 * serialized as their {@link Enum#name()} string to decouple the wire
 * format from the domain enum classes.</p>
 *
 * <p>{@code @JsonInclude(NON_NULL)} omits fields that are null for loans
 * in early lifecycle states (e.g. {@code approvedAmount} is null for a
 * loan still in {@code UNDER_REVIEW}; {@code disbursementDate} is null
 * until the loan is disbursed).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoanResponse(

        Long loanId,
        String loanType,
        UUID applicantClientId,
        BigDecimal requestedAmount,
        Integer termInMonths,
        String loanStatus,

        BigDecimal approvedAmount,
        BigDecimal interestRate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate approvalDate,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate disbursementDate,

        String disbursementAccountNumber
) {
}
