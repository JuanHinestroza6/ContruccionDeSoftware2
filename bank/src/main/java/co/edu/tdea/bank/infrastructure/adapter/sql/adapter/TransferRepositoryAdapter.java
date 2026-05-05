package co.edu.tdea.bank.infrastructure.adapter.sql.adapter;

import co.edu.tdea.bank.application.port.out.TransferRepositoryPort;
import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.domain.model.Transfer;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankAccountEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.TransferEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.mapper.TransferMapper;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankAccountJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.TransferJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransferRepositoryAdapter implements TransferRepositoryPort {

    private final TransferJpaRepository jpa;
    private final BankAccountJpaRepository bankAccountJpa;
    private final UserJpaRepository userJpa;

    public TransferRepositoryAdapter(TransferJpaRepository jpa,
                                     BankAccountJpaRepository bankAccountJpa,
                                     UserJpaRepository userJpa) {
        this.jpa = jpa;
        this.bankAccountJpa = bankAccountJpa;
        this.userJpa = userJpa;
    }

    @Override
    public Transfer save(Transfer transfer) {
        // Resolve all FK references as Hibernate proxies — no SELECT issued.
        // approvedBy is nullable: transfers in EN_ESPERA have no approver yet.
        BankAccountEntity sourceRef = bankAccountJpa.getReferenceById(
                transfer.getSourceAccount().getAccountNumber());
        BankAccountEntity destinationRef = bankAccountJpa.getReferenceById(
                transfer.getDestinationAccount().getAccountNumber());
        UserEntity createdByRef = userJpa.getReferenceById(
                transfer.getCreatedBy().getUserId());
        UserEntity approvedByRef = transfer.getApprovedBy() != null
                ? userJpa.getReferenceById(transfer.getApprovedBy().getUserId())
                : null;

        TransferEntity saved = jpa.save(
                TransferMapper.toEntity(transfer, sourceRef, destinationRef,
                        createdByRef, approvedByRef));
        return TransferMapper.toDomain(saved);
    }

    @Override
    public Optional<Transfer> findById(Long transferId) {
        return jpa.findById(transferId).map(TransferMapper::toDomain);
    }

    @Override
    public List<Transfer> findBySourceAccountNumber(String accountNumber) {
        return TransferMapper.toDomainList(
                jpa.findBySourceAccountAccountNumber(accountNumber));
    }

    @Override
    public List<Transfer> findByDestinationAccountNumber(String accountNumber) {
        return TransferMapper.toDomainList(
                jpa.findByDestinationAccountAccountNumber(accountNumber));
    }

    @Override
    public List<Transfer> findByStatus(TransferStatus status) {
        return TransferMapper.toDomainList(jpa.findByTransferStatus(status));
    }

    @Override
    public List<Transfer> findByCreatedByUserId(UUID userId) {
        return TransferMapper.toDomainList(jpa.findByCreatedByUserId(userId));
    }
}
