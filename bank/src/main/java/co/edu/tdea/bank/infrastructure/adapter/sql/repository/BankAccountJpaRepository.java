package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BankAccountJpaRepository extends JpaRepository<BankAccountEntity, String> {

    List<BankAccountEntity> findByHolderClientId(UUID clientId);

    List<BankAccountEntity> findByHolderClientIdAndAccountType(UUID clientId, AccountType accountType);

    List<BankAccountEntity> findByAccountStatus(AccountStatus status);

    boolean existsByAccountNumber(String accountNumber);
}
