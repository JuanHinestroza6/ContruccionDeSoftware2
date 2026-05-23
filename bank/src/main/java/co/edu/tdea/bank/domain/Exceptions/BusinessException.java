package co.edu.tdea.bank.domain.Exceptions;

/**
 * Base exception for all domain and application business rule violations.
 * Extend this class for every specific failure scenario in the system.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
