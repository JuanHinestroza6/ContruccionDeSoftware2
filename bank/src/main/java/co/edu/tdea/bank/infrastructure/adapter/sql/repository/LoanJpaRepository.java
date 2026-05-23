package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.domain.enums.LoanStatus;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanJpaRepository extends JpaRepository<LoanEntity, Long> {

    List<LoanEntity> findByApplicantClientClientId(UUID clientId);

    List<LoanEntity> findByLoanStatus(LoanStatus status);

    List<LoanEntity> findByApplicantClientClientIdAndLoanStatus(UUID clientId, LoanStatus status);
}
