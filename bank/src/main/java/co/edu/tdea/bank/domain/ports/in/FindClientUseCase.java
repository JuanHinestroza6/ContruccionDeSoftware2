package co.edu.tdea.bank.domain.ports.in;

import co.edu.tdea.bank.domain.models.Client;

import java.util.UUID;

/**
 * Input port — query operations for the {@link Client} aggregate.
 */
public interface FindClientUseCase {

    /**
     * Finds a client by its technical identifier.
     *
     * @param clientId technical UUID of the client
     * @return the client (individual or business)
     */
    Client findById(UUID clientId);

    /**
     * Finds a client by its national identification number (Cédula or NIT).
     *
     * @param identificationId identification number
     * @return the client (individual or business)
     */
    Client findByIdentificationId(String identificationId);
}
