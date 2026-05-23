package co.edu.tdea.bank.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * A Client that is a legal entity (company). Identified by its tax/registration
 * number and represented by a natural person (legalRepresentative).
 */
public final class BusinessClient extends Client {

    private final String companyName;
    private String legalRepresentative;

    private BusinessClient(UUID clientId, String identificationId,
                           String email, String phone, String address,
                           String companyName, String legalRepresentative) {
        super(clientId, identificationId, email, phone, address);
        this.companyName           = companyName;
        this.legalRepresentative   = legalRepresentative;
    }

    /** Constructor de rehidratación — sin validaciones de creación. */
    private BusinessClient(UUID clientId, String identificationId, String email,
                           String phone, String address,
                           String companyName, String legalRepresentative,
                           boolean reconstruct) {
        super(clientId, identificationId, email, phone, address, reconstruct);
        this.companyName         = companyName;
        this.legalRepresentative = legalRepresentative;
    }

    /**
     * Factory method — validates all invariants before constructing the object.
     */
    public static BusinessClient create(String identificationId,
                                        String email,
                                        String phone,
                                        String address,
                                        String companyName,
                                        String legalRepresentative) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("companyName must not be blank.");
        }
        if (legalRepresentative == null || legalRepresentative.isBlank()) {
            throw new IllegalArgumentException("legalRepresentative must not be blank.");
        }

        return new BusinessClient(
                UUID.randomUUID(),
                identificationId,
                email,
                phone,
                address,
                companyName.trim(),
                legalRepresentative.trim()
        );
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     */
    public static BusinessClient reconstruct(UUID clientId,
                                             String identificationId,
                                             String email,
                                             String phone,
                                             String address,
                                             String companyName,
                                             String legalRepresentative) {
        Objects.requireNonNull(companyName,         "companyName must not be null.");
        Objects.requireNonNull(legalRepresentative, "legalRepresentative must not be null.");
        return new BusinessClient(clientId, identificationId, email, phone, address,
                                  companyName, legalRepresentative, true);
    }

    public void updateLegalRepresentative(String legalRepresentative) {
        if (legalRepresentative == null || legalRepresentative.isBlank()) {
            throw new IllegalArgumentException("legalRepresentative must not be blank.");
        }
        this.legalRepresentative = legalRepresentative.trim();
    }

    public String getCompanyName()           { return companyName; }
    public String getLegalRepresentative()   { return legalRepresentative; }

    @Override
    public String toString() {
        return "BusinessClient{clientId=" + getClientId() + ", companyName='" + companyName
                + "', representative='" + legalRepresentative + "', email='" + getEmail() + "'}";
    }
}
