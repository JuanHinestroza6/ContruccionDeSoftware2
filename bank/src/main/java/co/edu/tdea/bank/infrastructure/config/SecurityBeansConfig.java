package co.edu.tdea.bank.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans related to security primitives that are independent from the HTTP
 * filter chain.
 *
 * <p>Kept in a dedicated {@code @Configuration} class so the {@link PasswordEncoder}
 * can be injected by application services (e.g. user-registration use cases)
 * without forcing them to depend on the full {@code SecurityConfig} graph.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * BCrypt is the project-mandated hashing algorithm for user credentials.
     * Never construct a {@code PasswordEncoder} ad-hoc anywhere else in the
     * code base — always inject this bean.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
