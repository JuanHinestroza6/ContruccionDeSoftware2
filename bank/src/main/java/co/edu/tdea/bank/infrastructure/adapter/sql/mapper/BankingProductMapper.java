package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.models.BankingProduct;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BankingProductEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts {@link BankingProduct} domain objects to/from JPA entities.
 * No Spring beans, no MapStruct.
 */
public final class BankingProductMapper {

    private BankingProductMapper() {}

    public static BankingProductEntity toEntity(BankingProduct product) {
        if (product == null) return null;
        BankingProductEntity entity = new BankingProductEntity();
        entity.setProductCode(product.getProductCode());
        entity.setProductName(product.getProductName());
        entity.setCategory(product.getCategory());
        entity.setRequiresApproval(product.isRequiresApproval());
        return entity;
    }

    public static BankingProduct toDomain(BankingProductEntity entity) {
        if (entity == null) return null;
        return BankingProduct.reconstruct(
                entity.getProductCode(),
                entity.getProductName(),
                entity.getCategory(),
                entity.isRequiresApproval()
        );
    }

    public static List<BankingProduct> toDomainList(List<BankingProductEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(BankingProductMapper::toDomain)
                .collect(Collectors.toList());
    }
}
