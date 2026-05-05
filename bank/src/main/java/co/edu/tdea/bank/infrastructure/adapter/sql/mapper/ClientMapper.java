package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.model.BusinessClient;
import co.edu.tdea.bank.domain.model.Client;
import co.edu.tdea.bank.domain.model.IndividualClient;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BusinessClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.IndividualClientEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts the polymorphic {@link Client} domain hierarchy
 * to/from the JPA {@code JOINED} inheritance entities.
 *
 * <p>Dispatches on {@code instanceof} for both directions:
 * <ul>
 *   <li>{@link IndividualClient} ↔ {@link IndividualClientEntity}</li>
 *   <li>{@link BusinessClient}   ↔ {@link BusinessClientEntity}</li>
 * </ul>
 */
public final class ClientMapper {

    private ClientMapper() {}

    // -------------------------------------------------------------------------
    // Domain -> Entity
    // -------------------------------------------------------------------------

    public static ClientEntity toEntity(Client client) {
        if (client == null) return null;
        if (client instanceof IndividualClient ind) {
            return toEntity(ind);
        }
        if (client instanceof BusinessClient biz) {
            return toEntity(biz);
        }
        throw new IllegalArgumentException(
                "Unknown Client subtype: " + client.getClass().getName());
    }

    public static IndividualClientEntity toEntity(IndividualClient client) {
        if (client == null) return null;
        IndividualClientEntity entity = new IndividualClientEntity();
        entity.setClientId(client.getClientId());
        entity.setIdentificationId(client.getIdentificationId());
        entity.setEmail(client.getEmail());
        entity.setPhone(client.getPhone());
        entity.setAddress(client.getAddress());
        entity.setFullName(client.getFullName());
        entity.setBirthDate(client.getBirthDate());
        return entity;
    }

    public static BusinessClientEntity toEntity(BusinessClient client) {
        if (client == null) return null;
        BusinessClientEntity entity = new BusinessClientEntity();
        entity.setClientId(client.getClientId());
        entity.setIdentificationId(client.getIdentificationId());
        entity.setEmail(client.getEmail());
        entity.setPhone(client.getPhone());
        entity.setAddress(client.getAddress());
        entity.setCompanyName(client.getCompanyName());
        entity.setLegalRepresentative(client.getLegalRepresentative());
        return entity;
    }

    // -------------------------------------------------------------------------
    // Entity -> Domain
    // -------------------------------------------------------------------------

    public static Client toDomain(ClientEntity entity) {
        if (entity == null) return null;
        if (entity instanceof IndividualClientEntity ind) {
            return toDomain(ind);
        }
        if (entity instanceof BusinessClientEntity biz) {
            return toDomain(biz);
        }
        throw new IllegalArgumentException(
                "Unknown ClientEntity subtype: " + entity.getClass().getName());
    }

    public static IndividualClient toDomain(IndividualClientEntity entity) {
        if (entity == null) return null;
        return IndividualClient.reconstruct(
                entity.getClientId(),
                entity.getIdentificationId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getFullName(),
                entity.getBirthDate()
        );
    }

    public static BusinessClient toDomain(BusinessClientEntity entity) {
        if (entity == null) return null;
        return BusinessClient.reconstruct(
                entity.getClientId(),
                entity.getIdentificationId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getCompanyName(),
                entity.getLegalRepresentative()
        );
    }

    // -------------------------------------------------------------------------
    // Bulk helpers
    // -------------------------------------------------------------------------

    public static List<IndividualClient> toIndividualDomainList(List<IndividualClientEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(ClientMapper::toDomain)
                .collect(Collectors.toList());
    }

    public static List<BusinessClient> toBusinessDomainList(List<BusinessClientEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(ClientMapper::toDomain)
                .collect(Collectors.toList());
    }
}
