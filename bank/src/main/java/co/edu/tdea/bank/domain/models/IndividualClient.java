package co.edu.tdea.bank.domain.models;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

/**
 * A Client who is a natural person. Validates that the person is at least
 * 18 years old at the time of registration.
 */
public final class IndividualClient extends Client {

    private static final int MINIMUM_AGE = 18;

    private final String fullName;
    private final LocalDate birthDate;

    private IndividualClient(UUID clientId, String identificationId,
                             String email, String phone, String address,
                             String fullName, LocalDate birthDate) {
        super(clientId, identificationId, email, phone, address);
        this.fullName  = fullName;
        this.birthDate = birthDate;
    }

    /** Constructor de rehidratación — sin validaciones de creación. */
    private IndividualClient(UUID clientId, String identificationId, String email,
                             String phone, String address,
                             String fullName, LocalDate birthDate, boolean reconstruct) {
        super(clientId, identificationId, email, phone, address, reconstruct);
        this.fullName  = fullName;
        this.birthDate = birthDate;
    }

    /**
     * Factory method — validates all invariants before constructing the object.
     */
    public static IndividualClient create(String identificationId,
                                          String email,
                                          String phone,
                                          String address,
                                          String fullName,
                                          LocalDate birthDate) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank.");
        }
        Objects.requireNonNull(birthDate, "birthDate must not be null.");

        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthDate must be a past date.");
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < MINIMUM_AGE) {
            throw new IllegalArgumentException(
                    "Client must be at least " + MINIMUM_AGE + " years old. Provided age: " + age);
        }

        return new IndividualClient(
                UUID.randomUUID(),
                identificationId,
                email,
                phone,
                address,
                fullName.trim(),
                birthDate
        );
    }

    /**
     * Reconstrucción desde almacenamiento. Asume invariantes ya validadas en creación original.
     */
    public static IndividualClient reconstruct(UUID clientId,
                                               String identificationId,
                                               String email,
                                               String phone,
                                               String address,
                                               String fullName,
                                               LocalDate birthDate) {
        Objects.requireNonNull(fullName,  "fullName must not be null.");
        Objects.requireNonNull(birthDate, "birthDate must not be null.");
        return new IndividualClient(clientId, identificationId, email, phone, address,
                                    fullName, birthDate, true);
    }

    public String getFullName()     { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return "IndividualClient{clientId=" + getClientId() + ", fullName='" + fullName
                + "', age=" + getAge() + ", email='" + getEmail() + "'}";
    }
}
