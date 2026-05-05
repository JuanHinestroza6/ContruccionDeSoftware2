package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.model.Loan;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.LoanEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts {@link Loan} domain objects to/from JPA entities.
 *
 * <p>Write path ({@link #toEntity}) does NOT translate related aggregates: the
 * adapter is responsible for resolving FK references via
 * {@code jpaRepository.getReferenceById(...)} and passing them in. This avoids
 * Hibernate trying to cascade-persist a Client or BankAccount that already
 * exists.
 *
 * <p>Read path ({@link #toDomain}) is free to walk the graph through the other
 * mappers — there is no risk of duplicate inserts on reads.
 */
public final class LoanMapper {

    private LoanMapper() {}

    /**
     * Maps a {@link Loan} aggregate to a JPA entity for persistence.
     *
     * @param loan                domain aggregate (must not be {@code null})
     * @param applicantRef        managed/proxy {@link ClientEntity} for the applicant FK
     *                            (obtained via {@code clientJpaRepository.getReferenceById(...)})
     * @param disbursementAccountRef managed/proxy {@link BankAccountEntity} for the
     *                            disbursement target FK, or {@code null} if the loan
     *                            has not been disbursed yet
     */
    public static LoanEntity toEntity(Loan loan,
                                      ClientEntity applicantRef,
                                      BankAccountEntity disbursementAccountRef) {
        if (loan == null) return null;
        LoanEntity entity = new LoanEntity();
        entity.setLoanId(loan.getLoanId());
        entity.setLoanType(loan.getLoanType());
        entity.setApplicantClient(applicantRef);
        entity.setRequestedAmount(loan.getRequestedAmount());
        entity.setTermInMonths(loan.getTermInMonths());
        entity.setApprovedAmount(loan.getApprovedAmount());
        entity.setInterestRate(loan.getInterestRate());
        entity.setLoanStatus(loan.getLoanStatus());
        entity.setApprovalDate(loan.getApprovalDate());
        entity.setDisbursementDate(loan.getDisbursementDate());
        entity.setDisbursementTargetAccount(disbursementAccountRef);
        return entity;
    }

    public static Loan toDomain(LoanEntity entity) {
        if (entity == null) return null;
        return Loan.reconstruct(
                entity.getLoanId(),
                entity.getLoanType(),
                ClientMapper.toDomain(entity.getApplicantClient()),
                entity.getRequestedAmount(),
                entity.getTermInMonths(),
                entity.getApprovedAmount(),
                entity.getInterestRate(),
                entity.getLoanStatus(),
                entity.getApprovalDate(),
                entity.getDisbursementDate(),
                BankAccountMapper.toDomain(entity.getDisbursementTargetAccount())
        );
    }

    public static List<Loan> toDomainList(List<LoanEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(LoanMapper::toDomain)
                .collect(Collectors.toList());
    }
}
