package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.domain.enums.ProductCategory;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankingProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankingProductJpaRepository extends JpaRepository<BankingProductEntity, String> {

    List<BankingProductEntity> findByCategory(ProductCategory category);

    List<BankingProductEntity> findByRequiresApprovalTrue();

    boolean existsByProductCode(String productCode);
}
