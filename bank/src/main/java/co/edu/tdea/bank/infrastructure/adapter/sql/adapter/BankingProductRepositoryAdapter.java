package co.edu.tdea.bank.infrastructure.adapter.sql.adapter;

import co.edu.tdea.bank.domain.ports.out.BankingProductRepositoryPort;
import co.edu.tdea.bank.domain.enums.ProductCategory;
import co.edu.tdea.bank.domain.models.BankingProduct;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankingProductEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.mapper.BankingProductMapper;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BankingProductJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BankingProductRepositoryAdapter implements BankingProductRepositoryPort {

    private final BankingProductJpaRepository jpa;

    public BankingProductRepositoryAdapter(BankingProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BankingProduct save(BankingProduct product) {
        BankingProductEntity saved = jpa.save(BankingProductMapper.toEntity(product));
        return BankingProductMapper.toDomain(saved);
    }

    @Override
    public Optional<BankingProduct> findByProductCode(String productCode) {
        return jpa.findById(productCode).map(BankingProductMapper::toDomain);
    }

    @Override
    public List<BankingProduct> findByCategory(ProductCategory category) {
        return BankingProductMapper.toDomainList(jpa.findByCategory(category));
    }

    @Override
    public List<BankingProduct> findAllRequiringApproval() {
        return BankingProductMapper.toDomainList(jpa.findByRequiresApprovalTrue());
    }

    @Override
    public List<BankingProduct> findAll() {
        return BankingProductMapper.toDomainList(jpa.findAll());
    }

    @Override
    public boolean existsByProductCode(String productCode) {
        return jpa.existsByProductCode(productCode);
    }
}
