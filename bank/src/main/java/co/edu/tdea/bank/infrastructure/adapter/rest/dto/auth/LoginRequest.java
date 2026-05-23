package co.edu.tdea.bank.infrastructure.adapter.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/login}.
 *
 * <p>Both fields are {@code @NotBlank} so callers receive a 400 instead of a
 * generic 401 when they forget a credential entirely. Note that the validation
 * error messages do NOT distinguish "missing username" from "missing password"
 * once we hit the authentication layer — that distinction is for malformed
 * requests only.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
