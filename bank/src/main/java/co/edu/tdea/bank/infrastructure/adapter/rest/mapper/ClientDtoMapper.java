package co.edu.tdea.bank.infrastructure.adapter.rest.mapper;

import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.client.ClientResponse;

/**
 * Static utility that converts a {@link Client} aggregate into the
 * polymorphic {@link ClientResponse} DTO. Fields that do not apply to a
 * given subtype are left {@code null}.
 */
public final class ClientDtoMapper {

    private static final String TYPE_INDIVIDUAL = "INDIVIDUAL";
    private static final String TYPE_BUSINESS   = "BUSINESS";

    private ClientDtoMapper() {
        // Utility class — no instances.
    }

    /**
     * Maps a domain {@link Client} (individual or business) to a
     * {@link ClientResponse}.
     *
     * @param client domain client to map; must not be {@code null}
     * @return the corresponding response DTO
     * @throws IllegalArgumentException if {@code client} is of an unsupported subtype
     */
    public static ClientResponse toResponse(Client client) {
        if (client instanceof IndividualClient individual) {
            return new ClientResponse(
                    individual.getClientId(),
                    individual.getIdentificationId(),
                    individual.getEmail(),
                    individual.getPhone(),
                    individual.getAddress(),
                    TYPE_INDIVIDUAL,
                    individual.getFullName(),
                    individual.getBirthDate(),
                    null,
                    null
            );
        }
        if (client instanceof BusinessClient business) {
            return new ClientResponse(
                    business.getClientId(),
                    business.getIdentificationId(),
                    business.getEmail(),
                    business.getPhone(),
                    business.getAddress(),
                    TYPE_BUSINESS,
                    null,
                    null,
                    business.getCompanyName(),
                    business.getLegalRepresentative()
            );
        }
        throw new IllegalArgumentException(
                "Unsupported Client subtype: " + client.getClass().getName());
    }
}
