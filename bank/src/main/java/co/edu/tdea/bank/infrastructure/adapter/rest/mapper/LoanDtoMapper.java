package co.edu.tdea.bank.infrastructure.adapter.rest.mapper;

import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.domain.models.Loan;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.loan.LoanResponse;

/**
 * Static utility that converts a {@link Loan} aggregate into its outbound
 * {@link LoanResponse} DTO. Enum-valued attributes are exposed using their
 * {@link Enum#name()} representation, and nullable lifecycle fields are
 * propagated as {@code null} (filtered out at serialization time by
 * {@code @JsonInclude(NON_NULL)} on the response record).
 */
public final class LoanDtoMapper {

    private LoanDtoMapper() {
        // Utility class — no instances.
    }

    /**
     * Maps a domain {@link Loan} to a {@link LoanResponse}.
     *
     * @param loan domain aggregate to map; must not be {@code null}
     * @return the corresponding response DTO
     */
    public static LoanResponse toResponse(Loan loan) {
        BankAccount disbursementAccount = loan.getDisbursementTargetAccount();
        String disbursementAccountNumber =
                disbursementAccount != null ? disbursementAccount.getAccountNumber() : null;

        return new LoanResponse(
                loan.getLoanId(),
                loan.getLoanType().name(),
                loan.getApplicantClient().getClientId(),
                loan.getRequestedAmount(),
                loan.getTermInMonths(),
                loan.getLoanStatus().name(),
                loan.getApprovedAmount(),
                loan.getInterestRate(),
                loan.getApprovalDate(),
                loan.getDisbursementDate(),
                disbursementAccountNumber
        );
    }
}
