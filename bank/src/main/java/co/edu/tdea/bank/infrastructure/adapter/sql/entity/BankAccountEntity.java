package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import co.edu.tdea.bank.domain.enums.AccountStatus;
import co.edu.tdea.bank.domain.enums.AccountType;
import co.edu.tdea.bank.domain.enums.CurrencyType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "bank_accounts", indexes = {
        @Index(name = "idx_bank_accounts_holder_client_id", columnList = "holder_client_id")
})
public class BankAccountEntity {

    @Id
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "holder_client_id", nullable = false)
    private ClientEntity holder;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyType currency;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;
}
