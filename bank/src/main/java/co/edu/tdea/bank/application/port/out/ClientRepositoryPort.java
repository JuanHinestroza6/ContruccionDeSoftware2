package co.edu.tdea.bank.application.port.out;

import co.edu.tdea.bank.domain.model.BusinessClient;
import co.edu.tdea.bank.domain.model.Client;
import co.edu.tdea.bank.domain.model.IndividualClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — persistence contract for {@link Client} aggregate and its subtypes.
 */
public interface ClientRepositoryPort {

    /** Persists a new client or updates an existing one. Returns the saved instance. */
    Client save(Client client);

    /** Finds any client (individual or business) by its technical identity. */
    Optional<Client> findById(UUID clientId);

    /** Finds any client by their identification number (CC or NIT). */
    Optional<Client> findByIdentificationId(String identificationId);

    /** Finds any client by email address. */
    Optional<Client> findByEmail(String email);

    /** Returns all individual (natural person) clients. */
    List<IndividualClient> findAllIndividualClients();

    /** Returns all business (legal entity) clients. */
    List<BusinessClient> findAllBusinessClients();

    /** Returns true if a client with the given identification number already exists. */
    boolean existsByIdentificationId(String identificationId);

    /** Returns true if a client with the given email already exists. */
    boolean existsByEmail(String email);
}
