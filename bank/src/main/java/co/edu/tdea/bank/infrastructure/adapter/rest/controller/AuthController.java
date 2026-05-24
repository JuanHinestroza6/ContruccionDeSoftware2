package co.edu.tdea.bank.infrastructure.adapter.rest.controller;

import co.edu.tdea.bank.infrastructure.adapter.rest.dto.auth.LoginRequest;
import co.edu.tdea.bank.infrastructure.adapter.rest.dto.auth.LoginResponse;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
import co.edu.tdea.bank.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Public authentication endpoint.
 *
 * <p>Exposes {@code POST /api/v1/auth/login} and is the only route in this
 * controller that lives in the {@code permitAll()} allow-list of
 * {@code SecurityConfig}. Everything else in the API must arrive with a valid
 * JWT.</p>
 *
 * <p>Error handling delegates to {@link AuthenticationManager}: failed
 * credentials, disabled accounts, and locked accounts surface as
 * {@link AuthenticationException} subclasses which the
 * {@code GlobalExceptionHandler} translates to HTTP status codes. We deliberately
 * do NOT distinguish "user not found" from "wrong password" to avoid leaking
 * account-enumeration signals.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login y obtención de token JWT")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserJpaRepository userJpaRepository;
    private final long expirationMinutes;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserJpaRepository userJpaRepository,
                          @org.springframework.beans.factory.annotation.Value("${bank.jwt.expiration-minutes}")
                          long expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userJpaRepository = userJpaRepository;
        this.expirationMinutes = expirationMinutes;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Delegating to the AuthenticationManager runs BankUserDetailsService +
        // BCrypt comparison. On failure it throws a subclass of
        // AuthenticationException, which the GlobalExceptionHandler turns into
        // an appropriate 401/403 — we never catch it here so we do not leak
        // diagnostic details into a response body.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // At this point the credentials are valid AND the account is ACTIVE.
        // Re-read the user row to grab the technical identifiers we embed in
        // the token; this is a cheap by-PK lookup that the JPA cache typically
        // serves from memory.
        UserEntity entity = userJpaRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user vanished between authenticate() and lookup"
                ));

        String role = entity.getSystemRole().name();
        String token = jwtService.generateToken(entity.getUserId(), entity.getUsername(), role);
        LocalDateTime expiresAt = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(expirationMinutes);

        log.info("Login success userId={} role={}", entity.getUserId(), role);

        return ResponseEntity.ok(new LoginResponse(token, expiresAt, role, entity.getUserId()));
    }
}
