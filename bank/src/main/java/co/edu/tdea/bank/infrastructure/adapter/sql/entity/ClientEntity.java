package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Discriminator column identifies whether the row belongs to INDIVIDUAL or BUSINESS subtype
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "client_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ClientEntity {

    @Id
    @Column(name = "client_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID clientId;

    @Column(name = "identification_id", nullable = false, unique = true)
    private String identificationId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "address", nullable = false)
    private String address;
}
