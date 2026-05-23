package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import co.edu.tdea.bank.domain.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "banking_products")
public class BankingProductEntity {

    @Id
    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ProductCategory category;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;
}
