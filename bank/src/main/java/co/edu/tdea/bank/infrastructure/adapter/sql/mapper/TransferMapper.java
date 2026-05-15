package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.models.Transfer;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.TransferEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts {@link Transfer} domain objects to/from JPA entities.
 *
 * <p>Write path ({@link #toEntity}) does NOT translate related aggregates: the
 * adapter is responsible for resolving FK references via
 * {@code jpaRepository.getReferenceById(...)} and passing them in. This avoids
 * Hibernate trying to cascade-persist BankAccount/User aggregates that already
 * exist.
 *
 * <p>Read path ({@link #toDomain}) is free to walk the graph through the other
 * mappers — there is no risk of duplicate inserts on reads.
 */
public final class TransferMapper {

    private TransferMapper() {}

    /**
     * Maps a {@link Transfer} aggregate to a JPA entity for persistence.
     *
     * @param transfer        domain aggregate (must not be {@code null})
     * @param sourceRef       managed/proxy {@link BankAccountEntity} for the source FK
     * @param destinationRef  managed/proxy {@link BankAccountEntity} for the destination FK
     * @param createdByRef    managed/proxy {@link UserEntity} for the createdBy FK
     * @param approvedByRef   managed/proxy {@link UserEntity} for the approvedBy FK,
     *                        or {@code null} if the transfer is still pending approval
     */
    public static TransferEntity toEntity(Transfer transfer,
                                          BankAccountEntity sourceRef,
                                          BankAccountEntity destinationRef,
                                          UserEntity createdByRef,
                                          UserEntity approvedByRef) {
        if (transfer == null) return null;
        TransferEntity entity = new TransferEntity();
        entity.setTransferId(transfer.getTransferId());
        entity.setSourceAccount(sourceRef);
        entity.setDestinationAccount(destinationRef);
        entity.setAmount(transfer.getAmount());
        entity.setCreationDateTime(transfer.getCreationDateTime());
        entity.setCreatedBy(createdByRef);
        entity.setApprovalDateTime(transfer.getApprovalDateTime());
        entity.setTransferStatus(transfer.getTransferStatus());
        entity.setApprovedBy(approvedByRef);
        return entity;
    }

    public static Transfer toDomain(TransferEntity entity) {
        if (entity == null) return null;
        return Transfer.reconstruct(
                entity.getTransferId(),
                BankAccountMapper.toDomain(entity.getSourceAccount()),
                BankAccountMapper.toDomain(entity.getDestinationAccount()),
                entity.getAmount(),
                entity.getCreationDateTime(),
                UserMapper.toDomain(entity.getCreatedBy()),
                entity.getApprovalDateTime(),
                entity.getTransferStatus(),
                UserMapper.toDomain(entity.getApprovedBy())
        );
    }

    public static List<Transfer> toDomainList(List<TransferEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(TransferMapper::toDomain)
                .collect(Collectors.toList());
    }
}
