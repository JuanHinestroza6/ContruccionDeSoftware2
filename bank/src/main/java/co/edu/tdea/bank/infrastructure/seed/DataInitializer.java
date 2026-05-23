package co.edu.tdea.bank.infrastructure.seed;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a fixed set of demo users (one per {@link SystemRole}) on application
 * startup so the team can exercise the auth flow locally without manually
 * inserting rows.
 *
 * <p><b>Why this seed bypasses {@code UserRepositoryPort} and writes directly to
 * {@link UserJpaRepository}:</b> the domain {@code User} aggregate intentionally
 * does NOT carry {@code username} / {@code password} — those are credentials
 * managed by the security adapter (see the Javadoc on
 * {@code co.edu.tdea.bank.infrastructure.adapter.sql.mapper.UserMapper}, which
 * explicitly leaves both fields untouched in {@code toEntity(...)}). Going
 * through {@code UserRepositoryPort.save(User)} would therefore persist a row
 * with {@code username = NULL} and {@code password = NULL}, which violates the
 * {@code NOT NULL} constraints on {@link UserEntity}. The seed lives in the
 * infrastructure layer and is allowed to talk to the JPA repository directly.
 *
 * <p>The seed is idempotent: each user is created only if no row exists with
 * the same {@code username}.
 *
 * <p>Disabled under the {@code test} profile so integration tests retain full
 * control over their fixtures.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserJpaRepository userJpaRepository,
                           PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<SeedUser> seeds = List.of(
                new SeedUser("cliente",    "Cliente123!",    SystemRole.INDIVIDUAL_CLIENT,    "Cliente Demo",    "cliente@bank.test",    "ID-CLI-001", "+57 300 0000001"),
                new SeedUser("empresa",    "Empresa123!",    SystemRole.BUSINESS_ADMIN,       "Empresa Demo",    "empresa@bank.test",    "ID-EMP-001", "+57 300 0000002"),
                new SeedUser("cajero",     "Cajero123!",     SystemRole.TELLER_EMPLOYEE,      "Cajero Demo",     "cajero@bank.test",     "ID-CAJ-001", "+57 300 0000003"),
                new SeedUser("comercial",  "Comercial123!",  SystemRole.COMMERCIAL_EMPLOYEE,  "Comercial Demo",  "comercial@bank.test",  "ID-COM-001", "+57 300 0000004"),
                new SeedUser("operador",   "Operador123!",   SystemRole.COMPANY_OPERATOR,     "Operador Demo",   "operador@bank.test",   "ID-OPE-001", "+57 300 0000005"),
                new SeedUser("supervisor", "Supervisor123!", SystemRole.COMPANY_SUPERVISOR,   "Supervisor Demo", "supervisor@bank.test", "ID-SUP-001", "+57 300 0000006"),
                new SeedUser("analista",   "Analista123!",   SystemRole.INTERNAL_ANALYST,     "Analista Demo",   "analista@bank.test",   "ID-ANA-001", "+57 300 0000007")
        );

        int created = 0;
        for (SeedUser seed : seeds) {
            if (userJpaRepository.existsByUsername(seed.username())) {
                continue;
            }
            userJpaRepository.save(toEntity(seed));
            created++;
        }

        if (created == 0) {
            log.info("Users already present, skipping seed");
        } else {
            log.info("Seeded {} users", created);
        }
    }

    private UserEntity toEntity(SeedUser seed) {
        UserEntity entity = new UserEntity();
        entity.setUserId(UUID.randomUUID());
        entity.setUsername(seed.username());
        // Hash with the project-mandated PasswordEncoder. The plain-text password
        // is consumed here and never logged.
        entity.setPassword(passwordEncoder.encode(seed.password()));
        entity.setRelatedClientId(null);
        entity.setSystemRole(seed.systemRole());
        entity.setUserStatus(UserStatus.ACTIVE);
        entity.setFullName(seed.fullName());
        entity.setIdentificationId(seed.identificationId());
        entity.setEmail(seed.email());
        entity.setPhone(seed.phone());
        entity.setBirthDate(LocalDate.of(1990, 1, 1));
        entity.setAddress("Calle Test 123, Medellin");
        return entity;
    }

    private record SeedUser(
            String username,
            String password,
            SystemRole systemRole,
            String fullName,
            String email,
            String identificationId,
            String phone
    ) {}
}
