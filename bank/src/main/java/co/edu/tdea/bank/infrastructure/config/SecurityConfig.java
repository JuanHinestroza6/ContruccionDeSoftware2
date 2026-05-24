package co.edu.tdea.bank.infrastructure.config;

import co.edu.tdea.bank.infrastructure.security.BankUserDetailsService;
import co.edu.tdea.bank.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * HTTP security baseline for the Bank API.
 *
 * <p>S2 scope: JWT authentication is now live end-to-end.
 * <ul>
 *   <li>Stateless session — every request is authenticated from scratch via JWT.</li>
 *   <li>CSRF disabled (justified below).</li>
 *   <li>CORS allow-list configured for local dev front-ends.</li>
 *   <li>{@code POST /api/v1/auth/login} and Swagger endpoints public; every
 *       other route requires authentication.</li>
 *   <li>{@code JwtAuthenticationFilter} installed before
 *       {@code UsernamePasswordAuthenticationFilter}.</li>
 *   <li>{@code DaoAuthenticationProvider} wired to {@code BankUserDetailsService}
 *       + the project {@code PasswordEncoder} (BCrypt).</li>
 * </ul>
 *
 * <p>S3 scope:
 * <ul>
 *   <li>{@code @EnableMethodSecurity(prePostEnabled = true)} turns on
 *       {@code @PreAuthorize} / {@code @PostAuthorize} on the controller
 *       layer. The full role-and-ownership matrix is applied per endpoint and
 *       backed by {@code AuthorizationService} (bean name {@code "authz"}).</li>
 *   <li>Failed authorization surfaces as {@code AccessDeniedException} and is
 *       translated to 403 by {@code GlobalExceptionHandler} — never 404, so we
 *       do not leak resource existence to non-owners.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF disabled: the API is stateless and authenticates via a
                // JWT carried in the Authorization header. Browsers do not
                // attach Authorization headers automatically (unlike session
                // cookies), so the classic CSRF threat model does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Override the default Spring Security 6 entry point. Without
                // this, an unauthenticated request would be funneled through
                // the AccessDeniedHandler and return 403 — but "no/invalid
                // token" should be 401, not 403. 403 is reserved for the
                // authorized-but-not-allowed case (handled in S3 via
                // @PreAuthorize / AccessDeniedException).
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Exposes the {@link AuthenticationManager} so the {@code AuthController}
     * can drive credential checks explicitly during {@code /login}.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * The DAO-backed authentication provider used by the
     * {@link AuthenticationManager}. Pairs {@link BankUserDetailsService} (which
     * applies INACTIVE/BLOCKED policy) with the project-mandated BCrypt
     * {@link PasswordEncoder}.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(BankUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * CORS allow-list. Explicit origins only — never use {@code "*"} because
     * combined with {@code allowCredentials=true} that would be rejected by
     * browsers and, more importantly, would defeat the purpose of the policy.
     *
     * <p>Origins listed here cover the dev front-ends commonly used by the
     * team (React on 3000, Angular on 4200, same-host Boot on 8080). Production
     * origins must be added explicitly when the deployment URL is known.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
