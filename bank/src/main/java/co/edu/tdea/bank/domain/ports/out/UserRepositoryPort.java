package co.edu.tdea.bank.domain.ports.out;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.models.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — persistence contract for {@link User} aggregate.
 */
public interface UserRepositoryPort {

    /** Persists a new user or updates an existing one. Returns the saved instance. */
    User save(User user);

    /** Finds a user by its technical identity. */
    Optional<User> findById(UUID userId);

    /**
     * Finds the system user linked to the given client.
     * Used to verify the user's status before allowing account operations on behalf of a client.
     */
    Optional<User> findByRelatedClientId(UUID clientId);

    /** Finds a user by their unique username (login handle). */
    Optional<User> findByUsername(String username);

    /** Returns all users that hold the given role. */
    List<User> findByRole(SystemRole role);

    /** Returns all users in the given status. */
    List<User> findByStatus(UserStatus status);

    /** Returns true if a user with the given username already exists. */
    boolean existsByUsername(String username);
}
