package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.model.BankAccount;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts {@link BankAccount} domain objects to/from JPA entities.
 *
 * <p>Write path ({@link #toEntity}) does NOT translate related aggregates: the
 * adapter is responsible for resolving FK references via
 * {@code jpaRepository.getReferenceById(...)} and passing them in. This avoids
 * Hibernate trying to cascade-persist a related aggregate that already exists.
 *
 * <p>Read path ({@link #toDomain}) is free to walk the graph through the other
 * mappers — there is no risk of duplicate inserts on reads.
 */
public final class BankAccountMapper {

    private BankAccountMapper() {}

    /**
     * Maps a {@link BankAccount} aggregate to a JPA entity for persistence.
     *
     * @param account   domain aggregate (must not be {@code null})
     * @param holderRef managed/proxy {@link ClientEntity} for the holder FK
     *                  (obtained via {@code clientJpaRepository.getReferenceById(holderId)})
     */
    public static BankAccountEntity toEntity(BankAccount account, ClientEntity holderRef) {
        if (account == null) return null;
        BankAccountEntity entity = new BankAccountEntity();
        entity.setAccountNumber(account.getAccountNumber());
        entity.setAccountType(account.getAccountType());
        entity.setHolder(holderRef);
        entity.setCurrentBalance(account.getCurrentBalance());
        entity.setCurrency(account.getCurrency());
        entity.setOpeningDate(account.getOpeningDate());
        entity.setAccountStatus(account.getAccountStatus());
        return entity;
    }

    public static BankAccount toDomain(BankAccountEntity entity) {
        if (entity == null) return null;
        return BankAccount.reconstruct(
                entity.getAccountNumber(),
                entity.getAccountType(),
                ClientMapper.toDomain(entity.getHolder()),
                entity.getCurrentBalance(),
                entity.getCurrency(),
                entity.getOpeningDate(),
                entity.getAccountStatus()
        );
    }

    public static List<BankAccount> toDomainList(List<BankAccountEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(BankAccountMapper::toDomain)
                .collect(Collectors.toList());
    }
}
