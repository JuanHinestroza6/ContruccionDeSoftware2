package co.edu.tdea.bank.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates JWTs for the Bank API.
 *
 * <p>Token payload (claims):
 * <ul>
 *   <li>{@code sub} — username (login handle)</li>
 *   <li>{@code userId} — UUID of the user (string form)</li>
 *   <li>{@code role} — {@link co.edu.tdea.bank.domain.enums.SystemRole} name</li>
 *   <li>{@code iat} / {@code exp} — issued-at and expiration timestamps</li>
 * </ul>
 *
 * <p>The signing key is derived from {@code bank.jwt.secret} via HMAC-SHA. The
 * secret is externalisable through the {@code JWT_SECRET} environment variable
 * and must be at least 32 bytes (256 bits) — HS256 mandates that minimum and
 * shorter keys are rejected here at startup so the failure surfaces immediately
 * instead of at the first request.
 *
 * <p>Tokens themselves are never logged. {@link #validateToken(String)} swallows
 * {@link JwtException} into a boolean so callers can branch without exposing
 * the underlying parse error to clients.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(@Value("${bank.jwt.secret}") String secret,
                      @Value("${bank.jwt.expiration-minutes}") long expirationMinutes) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "bank.jwt.secret must be at least 32 bytes (256 bits) for HS256. " +
                    "Set the JWT_SECRET environment variable to a longer value."
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = expirationMinutes * 60_000L;
    }

    /**
     * Issues a signed JWT for the given user. Caller is responsible for having
     * already authenticated the user — this method does not check credentials.
     */
    public String generateToken(UUID userId, String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMillis);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns {@code true} when the token signature and expiration are valid.
     * Never throws — invalid tokens are reported as {@code false} so the
     * caller (typically the {@code JwtAuthenticationFilter}) can simply skip
     * setting an authentication and let Spring return 401.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // We log the exception type but never the token contents.
            log.debug("JWT validation failed: {}", ex.getClass().getSimpleName());
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String raw = parseClaims(token).get(CLAIM_USER_ID, String.class);
        return UUID.fromString(raw);
    }

    public String extractRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
