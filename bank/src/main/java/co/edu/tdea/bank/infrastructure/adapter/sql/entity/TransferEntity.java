package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import co.edu.tdea.bank.domain.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfers_source_account", columnList = "source_account_number"),
        @Index(name = "idx_transfers_destination_account", columnList = "destination_account_number"),
        @Index(name = "idx_transfers_status", columnList = "transfer_status"),
        @Index(name = "idx_transfers_creation_date_time", columnList = "creation_date_time")
})
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_account_number", nullable = false)
    private BankAccountEntity sourceAccount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_account_number", nullable = false)
    private BankAccountEntity destinationAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "creation_date_time", nullable = false)
    private LocalDateTime creationDateTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdBy;

    @Column(name = "approval_date_time")
    private LocalDateTime approvalDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_status", nullable = false)
    private TransferStatus transferStatus;

    @ManyToOne(optional = true)
    @JoinColumn(name = "approved_by_user_id")
    private UserEntity approvedBy;
}
