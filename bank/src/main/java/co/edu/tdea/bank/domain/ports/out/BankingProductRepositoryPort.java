package co.edu.tdea.bank.domain.ports.out;

import co.edu.tdea.bank.domain.enums.ProductCategory;
import co.edu.tdea.bank.domain.models.BankingProduct;

import java.util.List;
import java.util.Optional;

/**
 * Output port — persistence contract for {@link BankingProduct} catalog.
 */
public interface BankingProductRepositoryPort {

    /** Persists a new product or updates an existing one. Returns the saved instance. */
    BankingProduct save(BankingProduct product);

    /** Finds a product by its natural business key. */
    Optional<BankingProduct> findByProductCode(String productCode);

    /** Returns all products in the given category. */
    List<BankingProduct> findByCategory(ProductCategory category);

    /** Returns all products that require explicit approval before being granted. */
    List<BankingProduct> findAllRequiringApproval();

    /** Returns all products in the catalog. */
    List<BankingProduct> findAll();

    /** Returns true if a product with the given code already exists. */
    boolean existsByProductCode(String productCode);
}
