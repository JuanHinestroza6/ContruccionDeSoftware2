package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.domain.enums.TransferStatus;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransferJpaRepository extends JpaRepository<TransferEntity, Long> {

    List<TransferEntity> findBySourceAccountAccountNumber(String accountNumber);

    List<TransferEntity> findByDestinationAccountAccountNumber(String accountNumber);

    List<TransferEntity> findByTransferStatus(TransferStatus status);

    List<TransferEntity> findByCreatedByUserId(UUID userId);

    List<TransferEntity> findByTransferStatusAndCreationDateTimeBefore(TransferStatus status, LocalDateTime cutoff);
}
