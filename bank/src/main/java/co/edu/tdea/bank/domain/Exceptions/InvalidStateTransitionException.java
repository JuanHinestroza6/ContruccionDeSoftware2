package co.edu.tdea.bank.domain.Exceptions;

/**
 * Thrown when an operation attempts an illegal state transition on an aggregate.
 * Examples: approving a loan that is already REJECTED, disbursing a transfer
 * that is EXPIRED, canceling an account that is already CANCELED.
 */
public class InvalidStateTransitionException extends BusinessException {

    public InvalidStateTransitionException(String aggregateType, Object currentState, Object attemptedOperation) {
        super("Cannot perform '" + attemptedOperation + "' on " + aggregateType
                + " in state: " + currentState);
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
