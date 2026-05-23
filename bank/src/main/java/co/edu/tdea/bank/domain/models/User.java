package co.edu.tdea.bank.domain.models;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an authenticated system user with access credentials and a role.
 * A User may be linked to a Client (the banking entity that holds products),
 * but they are separate concepts: a User can exist without a Client (e.g. an
 * internal teller), and a Client can exist without a User account.
 */
public final class User {

    private final UUID userId;
    private final UUID relatedClientId;   // nullable — not all users are bank clients
    private final String fullName;
    private final String identificationId;
    private final String email;
    private final String phone;
    private final LocalDate birthDate;
    private final String address;
    private SystemRole systemRole;
    private UserStatus userStatus;

    private User(Builder builder) {
        this.userId           = builder.userId;
        this.relatedClientId  = builder.relatedClientId;
        this.fullName         = builder.fullName;
        this.identificationId = builder.identificationId;
        this.email            = builder.email;
        this.phone            = builder.phone;
        this.birthDate        = builder.birthDate;
        this.address          = builder.address;
        this.systemRole       = builder.systemRole;
        this.userStatus       = builder.userStatus;
    }

    // --- State transitions ---

    public void activate() {
        this.userStatus = UserStatus.ACTIVE;
    }

    public void block() {
        if (this.userStatus == UserStatus.ACTIVE) {
            this.userStatus = UserStatus.BLOCKED;
        } else {
            throw new IllegalStateException("Only ACTIVE users can be blocked.");
        }
    }

    public void deactivate() {
        this.userStatus = UserStatus.INACTIVE;
    }

    public void changeRole(SystemRole newRole) {
        Objects.requireNonNull(newRole, "System role must not be null.");
        this.systemRole = newRole;
    }

    // --- Getters ---

    public UUID getUserId()              { return userId; }
    public UUID getRelatedClientId()     { return relatedClientId; }
    public String getFullName()          { return fullName; }
    public String getIdentificationId()  { return identificationId; }
    public String getEmail()             { return email; }
    public String getPhone()             { return phone; }
    public LocalDate getBirthDate()      { return birthDate; }
    public String getAddress()           { return address; }
    public SystemRole getSystemRole()    { return systemRole; }
    public UserStatus getUserStatus()    { return userStatus; }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UUID userId          = UUID.randomUUID();
        private UUID relatedClientId;
        private String fullName;
        private String identificationId;
        private String email;
        private String phone;
        private LocalDate birthDate;
        private String address;
        private SystemRole systemRole;
        private UserStatus userStatus = UserStatus.ACTIVE;

        private Builder() {}

        public Builder userId(UUID userId) {
            this.userId = Objects.requireNonNull(userId, "userId must not be null.");
            return this;
        }

        public Builder relatedClientId(UUID relatedClientId) {
            this.relatedClientId = relatedClientId;
            return this;
        }

        public Builder fullName(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException("fullName must not be blank.");
            }
            this.fullName = fullName.trim();
            return this;
        }

        public Builder identificationId(String identificationId) {
            if (identificationId == null || identificationId.isBlank()) {
                throw new IllegalArgumentException("identificationId must not be blank.");
            }
            this.identificationId = identificationId.trim();
            return this;
        }

        public Builder email(String email) {
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("email is invalid: " + email);
            }
            this.email = email.trim().toLowerCase();
            return this;
        }

        public Builder phone(String phone) {
            if (phone == null || phone.isBlank()) {
                throw new IllegalArgumentException("phone must not be blank.");
            }
            this.phone = phone.trim();
            return this;
        }

        public Builder birthDate(LocalDate birthDate) {
            // birthDate is optional for Users that represent internal bank
            // employees (e.g. tellers). The "must be of legal age" rule lives
            // exclusively on IndividualClient, not on User.
            if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("birthDate must be a past date.");
            }
            this.birthDate = birthDate;
            return this;
        }

        public Builder address(String address) {
            if (address == null || address.isBlank()) {
                throw new IllegalArgumentException("address must not be blank.");
            }
            this.address = address.trim();
            return this;
        }

        public Builder systemRole(SystemRole systemRole) {
            this.systemRole = Objects.requireNonNull(systemRole, "systemRole must not be null.");
            return this;
        }

        public Builder userStatus(UserStatus userStatus) {
            this.userStatus = Objects.requireNonNull(userStatus, "userStatus must not be null.");
            return this;
        }

        public User build() {
            Objects.requireNonNull(fullName,         "fullName is required.");
            Objects.requireNonNull(identificationId, "identificationId is required.");
            Objects.requireNonNull(email,            "email is required.");
            Objects.requireNonNull(phone,            "phone is required.");
            Objects.requireNonNull(address,          "address is required.");
            Objects.requireNonNull(systemRole,       "systemRole is required.");
            return new User(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", email='" + email + "', role=" + systemRole + ", status=" + userStatus + '}';
    }
}
