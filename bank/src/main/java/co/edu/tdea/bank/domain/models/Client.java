package co.edu.tdea.bank.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Abstract aggregate root representing a banking client — the entity that
 * holds products (accounts, loans, etc.). A Client is independent of User:
 * a corporate entity may have multiple Users operating on its behalf, and
 * a User may exist in the system without ever being a Client.
 */
public abstract class Client {

    private final UUID clientId;
    private final String identificationId;
    private String email;
    private String phone;
    private String address;

    protected Client(UUID clientId, String identificationId,
                     String email, String phone, String address) {
        Objects.requireNonNull(clientId, "clientId must not be null.");
        if (identificationId == null || identificationId.isBlank()) {
            throw new IllegalArgumentException("identificationId must not be blank.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email is invalid: " + email);
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank.");
        }
        this.clientId         = clientId;
        this.identificationId = identificationId.trim();
        this.email            = email.trim().toLowerCase();
        this.phone            = phone.trim();
        this.address          = address.trim();
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     * Constructor protegido sin trimming/normalización ni reglas de creación —
     * usado por las subclases concretas en sus métodos {@code reconstruct(...)}.
     */
    protected Client(UUID clientId, String identificationId, String email,
                     String phone, String address, boolean reconstruct) {
        Objects.requireNonNull(clientId,         "clientId must not be null.");
        Objects.requireNonNull(identificationId, "identificationId must not be null.");
        Objects.requireNonNull(email,            "email must not be null.");
        Objects.requireNonNull(phone,            "phone must not be null.");
        Objects.requireNonNull(address,          "address must not be null.");
        this.clientId         = clientId;
        this.identificationId = identificationId;
        this.email            = email;
        this.phone            = phone;
        this.address          = address;
    }

    // --- Contact update ---

    public void updateContact(String email, String phone, String address) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email is invalid: " + email);
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone must not be blank.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank.");
        }
        this.email   = email.trim().toLowerCase();
        this.phone   = phone.trim();
        this.address = address.trim();
    }

    // --- Getters ---

    public UUID getClientId()           { return clientId; }
    public String getIdentificationId() { return identificationId; }
    public String getEmail()            { return email; }
    public String getPhone()            { return phone; }
    public String getAddress()          { return address; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(clientId, client.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
