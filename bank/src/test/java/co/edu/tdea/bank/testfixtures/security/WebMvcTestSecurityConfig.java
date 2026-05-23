package co.edu.tdea.bank.testfixtures.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security wiring for {@code @WebMvcTest} controller slice tests.
 *
 * <p>The production {@link co.edu.tdea.bank.infrastructure.config.SecurityConfig}
 * depends on {@code JwtAuthenticationFilter}, which in turn pulls
 * {@code JwtService}, {@code BankUserDetailsService}, and {@code UserJpaRepository}
 * — none of which live in a controller slice. To keep the slice small and
 * deterministic, slice tests exclude {@code SecurityConfig} (via
 * {@code @WebMvcTest(excludeFilters=...)}) and import this stand-alone
 * {@link TestConfiguration} instead.
 *
 * <p>This config contributes:
 * <ul>
 *   <li>A {@link SecurityFilterChain} with the same defensive posture as
 *       production (stateless, CSRF off) but no JWT filter — the
 *       {@code SecurityContext} is populated by Spring Security Test via
 *       {@code @WithMockUser} on each test.</li>
 *   <li>{@link EnableMethodSecurity} so {@code @PreAuthorize} is honoured
 *       exactly as in production. This is the whole point of the slice: prove
 *       that the role gate works, not the JWT pipeline (covered by
 *       {@code AuthSecurityTest}).</li>
 * </ul>
 *
 * <p>Slice tests that rely on this config use {@code @WithMockUser(roles=...)}
 * to drive the {@code @PreAuthorize} matrix. The {@code @authz.*} ownership
 * expressions evaluate through {@code AuthorizationService}, which slice tests
 * neutralise with {@code @MockBean AuthorizationService} or by selecting a
 * staff role that short-circuits the {@code hasRole(...) or @authz.*} guard.
 */
@TestConfiguration
@EnableMethodSecurity
public class WebMvcTestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
}
