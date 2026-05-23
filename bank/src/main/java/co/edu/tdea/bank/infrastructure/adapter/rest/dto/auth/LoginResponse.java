package co.edu.tdea.bank.infrastructure.adapter.rest.dto.auth;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Successful login payload. The token MUST be carried back to subsequent
 * requests in the {@code Authorization: Bearer <token>} header.
 */
public record LoginResponse(
        String token,
        LocalDateTime expiresAt,
        String role,
        UUID userId
) {}
