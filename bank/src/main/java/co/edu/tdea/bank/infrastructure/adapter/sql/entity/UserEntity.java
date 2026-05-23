package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_related_client_id", columnList = "related_client_id")
})
public class UserEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "related_client_id", columnDefinition = "BINARY(16)")
    private UUID relatedClientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole systemRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus;

    // --- Personal profile fields (Option A — User owns its own profile) ---
    // identificationId / email do NOT carry UNIQUE at DB level; uniqueness is
    // enforced at the application layer (decision confirmed by the team).

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "identification_id", nullable = false)
    private String identificationId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    // nullable = true — internal staff users may not provide a birth date.
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "address", nullable = false)
    private String address;
}
