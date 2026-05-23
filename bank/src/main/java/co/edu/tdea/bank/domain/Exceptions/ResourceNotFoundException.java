package co.edu.tdea.bank.domain.Exceptions;

/**
 * Thrown when a requested resource does not exist in the system.
 * Examples: loan not found by ID, account number not registered, unknown client.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(resourceType + " not found with id: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
