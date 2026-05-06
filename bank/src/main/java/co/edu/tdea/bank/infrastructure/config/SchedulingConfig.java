package co.edu.tdea.bank.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's annotation-driven scheduling support.
 *
 * <p>Kept as a dedicated {@code @Configuration} class (instead of annotating
 * {@code BankApplication}) so the bootstrap stays minimal and so this feature
 * can be excluded from sliced test contexts when timing-driven jobs would
 * interfere with test determinism.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
