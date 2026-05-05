package co.edu.tdea.bank.infrastructure.adapter.sql.adapter;

import co.edu.tdea.bank.application.port.out.LoanRepositoryPort;
import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.domain.model.Loan;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.LoanEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.mapper.LoanMapper;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankAccountJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.LoanJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LoanRepositoryAdapter implements LoanRepositoryPort {

    private final LoanJpaRepository jpa;
    private final ClientJpaRepository clientJpa;
    private final BankAccountJpaRepository bankAccountJpa;

    public LoanRepositoryAdapter(LoanJpaRepository jpa,
                                 ClientJpaRepository clientJpa,
                                 BankAccountJpaRepository bankAccountJpa) {
        this.jpa = jpa;
        this.clientJpa = clientJpa;
        this.bankAccountJpa = bankAccountJpa;
    }

    @Override
    public Loan save(Loan loan) {
        // Resolve FK references as Hibernate proxies — no SELECT issued.
        // The disbursement target is nullable: a loan only points to a target
        // account once it reaches the DESEMBOLSADO state.
        ClientEntity applicantRef = clientJpa.getReferenceById(
                loan.getApplicantClient().getClientId());
        BankAccountEntity disbursementRef = loan.getDisbursementTargetAccount() != null
                ? bankAccountJpa.getReferenceById(
                        loan.getDisbursementTargetAccount().getAccountNumber())
                : null;

        LoanEntity saved = jpa.save(LoanMapper.toEntity(loan, applicantRef, disbursementRef));
        return LoanMapper.toDomain(saved);
    }

    @Override
    public Optional<Loan> findById(Long loanId) {
        return jpa.findById(loanId).map(LoanMapper::toDomain);
    }

    @Override
    public List<Loan> findByClientId(UUID clientId) {
        return LoanMapper.toDomainList(jpa.findByApplicantClientClientId(clientId));
    }

    @Override
    public List<Loan> findByStatus(LoanStatus status) {
        return LoanMapper.toDomainList(jpa.findByLoanStatus(status));
    }

    @Override
    public List<Loan> findByClientIdAndStatus(UUID clientId, LoanStatus status) {
        return LoanMapper.toDomainList(
                jpa.findByApplicantClientClientIdAndLoanStatus(clientId, status));
    }
}
