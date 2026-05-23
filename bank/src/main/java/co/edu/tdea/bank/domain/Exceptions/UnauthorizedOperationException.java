package co.edu.tdea.bank.domain.Exceptions;

/**
 * Thrown when a user attempts an operation they are not permitted to perform.
 * Examples: a BLOCKED user trying to open an account, a teller approving a loan
 * without the required role.
 */
public class UnauthorizedOperationException extends BusinessException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String userId, String operation) {
        super("User '" + userId + "' is not authorized to perform: " + operation);
    }
}
