package co.edu.tdea.bank.domain.model;

import co.edu.tdea.bank.domain.enums.ProductCategory;

import java.util.Objects;

/**
 * Catalog entry describing a financial product offered by the bank.
 * Immutable — once a product is defined its core attributes do not change.
 * If the bank retires or modifies a product, a new instance is created.
 */
public final class BankingProduct {

    private final String productCode;
    private final String productName;
    private final ProductCategory category;
    private final boolean requiresApproval;

    private BankingProduct(String productCode, String productName,
                           ProductCategory category, boolean requiresApproval) {
        this.productCode      = productCode;
        this.productName      = productName;
        this.category         = category;
        this.requiresApproval = requiresApproval;
    }

    /**
     * Factory method — validates all invariants before constructing the object.
     *
     * @param productCode     unique alphanumeric code that identifies the product
     * @param productName     human-readable name shown to clients
     * @param category        product classification ({@link ProductCategory})
     * @param requiresApproval whether opening/granting this product needs explicit approval
     */
    public static BankingProduct create(String productCode,
                                        String productName,
                                        ProductCategory category,
                                        boolean requiresApproval) {
        if (productCode == null || productCode.isBlank()) {
            throw new IllegalArgumentException("productCode must not be blank.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName must not be blank.");
        }
        Objects.requireNonNull(category, "category must not be null.");

        return new BankingProduct(
                productCode.trim().toUpperCase(),
                productName.trim(),
                category,
                requiresApproval
        );
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     */
    public static BankingProduct reconstruct(String productCode,
                                             String productName,
                                             ProductCategory category,
                                             boolean requiresApproval) {
        Objects.requireNonNull(productCode, "productCode must not be null.");
        Objects.requireNonNull(productName, "productName must not be null.");
        Objects.requireNonNull(category,    "category must not be null.");
        return new BankingProduct(productCode, productName, category, requiresApproval);
    }

    // --- Getters ---

    public String getProductCode()       { return productCode; }
    public String getProductName()       { return productName; }
    public ProductCategory getCategory() { return category; }
    public boolean isRequiresApproval()  { return requiresApproval; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankingProduct that)) return false;
        return Objects.equals(productCode, that.productCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productCode);
    }

    @Override
    public String toString() {
        return "BankingProduct{code='" + productCode + "', name='" + productName
                + "', category=" + category + ", requiresApproval=" + requiresApproval + '}';
    }
}
