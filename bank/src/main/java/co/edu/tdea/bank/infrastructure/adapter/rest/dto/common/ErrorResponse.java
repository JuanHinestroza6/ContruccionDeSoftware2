package co.edu.tdea.bank.infrastructure.adapter.rest.dto.common;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform HTTP error payload returned by {@code GlobalExceptionHandler}.
 *
 * <p>The {@code fieldErrors} attribute is {@code null} for non-validation
 * errors and populated (field -> default message) only for
 * {@code MethodArgumentNotValidException}.</p>
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    /**
     * Factory for general errors (no field-level details).
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    /**
     * Factory for bean-validation errors. Always builds a 400 response with
     * the {@code VALIDATION_ERROR} code and the per-field message map.
     */
    public static ErrorResponse ofValidation(String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), 400, "VALIDATION_ERROR", message, path, fieldErrors);
    }
}
