package co.edu.tdea.bank.infrastructure.adapter.sql.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "business_clients")
@DiscriminatorValue("BUSINESS")
public class BusinessClientEntity extends ClientEntity {

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "legal_representative", nullable = false)
    private String legalRepresentative;
}
