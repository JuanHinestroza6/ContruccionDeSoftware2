package co.edu.tdea.bank.infrastructure.security;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end security tests for the Bank API (S4 closure).
 *
 * <p>Drives the full JWT pipeline through {@link MockMvc}:
 * <ul>
 *   <li>{@code /api/v1/auth/login} — credential validation, account-status policy,
 *       account-enumeration protection.</li>
 *   <li>Protected resource access without/with invalid tokens.</li>
 *   <li>{@code @PreAuthorize} role gate: a valid token whose role does not
 *       satisfy the controller's matrix must yield 403, not 401.</li>
 * </ul>
 *
 * <p>The test seeds its own users via {@link UserJpaRepository} +
 * {@link PasswordEncoder} (BCrypt) in {@code @BeforeEach} because the
 * {@code DataInitializer} is annotated {@code @Profile("!test")} — the production
 * seed users do not exist under the test profile. Cleanup runs in
 * {@code @AfterEach}.
 *
 * <p>No password, no JWT, and no Authorization header content is ever logged
 * from this test or from production code paths it exercises.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "bank.transfers.expiration-window-minutes=60",
        "bank.transfers.expiration-job-rate-ms=600000"
})
class AuthSecurityTest {

    private static final String ANALYST_USERNAME = "analista-sec";
    private static final String TELLER_USERNAME  = "cajero-sec";
    private static final String BLOCKED_USERNAME = "bloqueado-sec";
    private static final String VALID_PASSWORD   = "Secret123!Sec";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedUsers() {
        userJpaRepository.deleteAll();

        userJpaRepository.saveAndFlush(staffUser(
                ANALYST_USERNAME, SystemRole.INTERNAL_ANALYST, UserStatus.ACTIVE,
                "Alice Analyst", "alice.analyst.sec@example.com"));

        userJpaRepository.saveAndFlush(staffUser(
                TELLER_USERNAME, SystemRole.TELLER_EMPLOYEE, UserStatus.ACTIVE,
                "Tom Teller", "tom.teller.sec@example.com"));

        userJpaRepository.saveAndFlush(staffUser(
                BLOCKED_USERNAME, SystemRole.INTERNAL_ANALYST, UserStatus.BLOCKED,
                "Bob Blocked", "bob.blocked.sec@example.com"));
    }

    @AfterEach
    void clearUsers() {
        userJpaRepository.deleteAll();
    }

    private UserEntity staffUser(String username,
                                 SystemRole role,
                                 UserStatus status,
                                 String fullName,
                                 String email) {
        UserEntity entity = new UserEntity();
        entity.setUserId(UUID.randomUUID());
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(VALID_PASSWORD));
        entity.setSystemRole(role);
        entity.setUserStatus(status);
        entity.setFullName(fullName);
        entity.setIdentificationId("ID-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setEmail(email);
        entity.setPhone("+57 300 0000099");
        entity.setAddress("HQ, Medellin");
        entity.setBirthDate(LocalDate.of(1990, 1, 1));
        return entity;
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    // =========================================================================
    // 1. Happy path login
    // =========================================================================
    @Test
    void should_return_200_with_token_when_login_with_valid_credentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", ANALYST_USERNAME, "password", VALID_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("INTERNAL_ANALYST"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    // =========================================================================
    // 2. Bad password
    // =========================================================================
    @Test
    void should_return_401_when_login_with_wrong_password() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", ANALYST_USERNAME, "password", "WrongPwd!"))))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 3. Unknown user — must be indistinguishable from wrong password (no
    //    enumeration leak)
    // =========================================================================
    @Test
    void should_return_401_when_login_with_nonexistent_user() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "ghost-user", "password", VALID_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 4. Protected endpoint without token → 401
    // =========================================================================
    @Test
    void should_return_401_when_accessing_protected_endpoint_without_token() throws Exception {
        mockMvc.perform(get("/api/v1/audit/by-entity")
                        .param("entityType", "Loan")
                        .param("entityId", "1"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 5. Protected endpoint with bogus token → 401
    // =========================================================================
    @Test
    void should_return_401_when_accessing_protected_endpoint_with_invalid_token() throws Exception {
        mockMvc.perform(get("/api/v1/audit/by-entity")
                        .param("entityType", "Loan")
                        .param("entityId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 6. Valid token, wrong role for the endpoint → 403
    //    Teller authenticates fine but PATCH /loans/{id}/approve is reserved for
    //    INTERNAL_ANALYST, so the @PreAuthorize gate must reject with 403.
    // =========================================================================
    @Test
    void should_return_403_when_role_does_not_match_endpoint() throws Exception {
        String tellerToken = login(TELLER_USERNAME, VALID_PASSWORD);

        // The path variable is arbitrary — the role gate fires BEFORE the
        // controller body executes, so the loan does not need to exist.
        String body = """
                {
                  "approverId": "00000000-0000-0000-0000-000000000001",
                  "approvedAmount": 1000000,
                  "interestRate": 0.15,
                  "approvalDate": "2026-01-01"
                }
                """;

        mockMvc.perform(patch("/api/v1/loans/{loanId}/approve", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 7. Blocked user must NEVER receive a valid token.
    //
    //    BankUserDetailsService throws LockedException for UserStatus.BLOCKED.
    //    Spring's DaoAuthenticationProvider rewraps that into
    //    InternalAuthenticationServiceException (a subclass of
    //    AuthenticationException), which the GlobalExceptionHandler's catch-all
    //    AuthenticationException mapping turns into 401 UNAUTHORIZED.
    //    The contract we assert here is the one that matters to the client:
    //    "the API must not issue a token to a blocked account" — i.e. anything
    //    other than 2xx. We pin 401 because that is the current production
    //    behaviour and the value the front-end is wired against.
    // =========================================================================
    @Test
    void should_reject_blocked_user_login_attempt() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", BLOCKED_USERNAME, "password", VALID_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }
}
