package co.edu.tdea.bank.infrastructure.adapter.sql.adapter;

import co.edu.tdea.bank.domain.ports.out.BankAccountRepositoryPort;
import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.models.BankAccount;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.mapper.BankAccountMapper;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankAccountJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BankAccountRepositoryAdapter implements BankAccountRepositoryPort {

    private final BankAccountJpaRepository jpa;
    private final ClientJpaRepository clientJpa;

    public BankAccountRepositoryAdapter(BankAccountJpaRepository jpa,
                                        ClientJpaRepository clientJpa) {
        this.jpa = jpa;
        this.clientJpa = clientJpa;
    }

    @Override
    public BankAccount save(BankAccount account) {
        // Resolve the holder FK as a Hibernate proxy â€” no SELECT is issued.
        // This prevents the mapper from rebuilding a transient ClientEntity
        // graph that Hibernate would otherwise try to cascade-persist.
        ClientEntity holderRef = clientJpa.getReferenceById(account.getHolder().getClientId());
        BankAccountEntity saved = jpa.save(BankAccountMapper.toEntity(account, holderRef));
        return BankAccountMapper.toDomain(saved);
    }

    @Override
    public Optional<BankAccount> findByAccountNumber(String accountNumber) {
        return jpa.findById(accountNumber).map(BankAccountMapper::toDomain);
    }

    @Override
    public List<BankAccount> findByClientId(UUID clientId) {
        return BankAccountMapper.toDomainList(jpa.findByHolderClientId(clientId));
    }

    @Override
    public List<BankAccount> findByClientIdAndType(UUID clientId, AccountType accountType) {
        return BankAccountMapper.toDomainList(
                jpa.findByHolderClientIdAndAccountType(clientId, accountType));
    }

    @Override
    public List<BankAccount> findByStatus(AccountStatus status) {
        return BankAccountMapper.toDomainList(jpa.findByAccountStatus(status));
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpa.existsByAccountNumber(accountNumber);
    }
}
