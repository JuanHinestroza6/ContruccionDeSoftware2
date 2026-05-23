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
        // BankAccountEntity has @Version (nullable Long). Spring Data's isNew()
        // returns true whenever version == null, which would route every save()
        // through em.persist() and break updates (DuplicateKey on the PK).
        //
        // The mapper has no concept of persistence version, so the only way to
        // preserve it across updates is to load the managed entity first and
        // mutate ONLY the fields that the domain considers mutable.
        Optional<BankAccountEntity> existing = jpa.findById(account.getAccountNumber());
        if (existing.isPresent()) {
            BankAccountEntity managed = existing.get();
            // Mutable fields only — accountNumber (PK), accountType, holder,
            // currency, openingDate and version are immutable post-INSERT.
            managed.setCurrentBalance(account.getCurrentBalance());
            managed.setAccountStatus(account.getAccountStatus());
            BankAccountEntity saved = jpa.save(managed);
            return BankAccountMapper.toDomain(saved);
        }

        // First-time INSERT path: resolve the holder FK as a Hibernate proxy so
        // no SELECT is issued and the mapper cannot trigger a cascade-persist
        // of an existing ClientEntity graph.
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
