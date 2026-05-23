package co.edu.tdea.bank.infrastructure.security;

import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges Spring Security's authentication machinery to the bank's user store.
 *
 * <p><b>Why this service reads {@link UserJpaRepository} directly instead of
 * going through {@code UserRepositoryPort}:</b> the domain {@code User}
 * aggregate intentionally does NOT carry {@code username} / {@code password}.
 * Those are credential fields owned by the security adapter (the same
 * justification documented on {@code DataInitializer}). Going through the
 * domain port would force us to either pollute the aggregate with security
 * concerns or to perform a second lookup just to retrieve the password hash.
 * Reading the JPA entity here keeps the domain clean and the credential check
 * single-shot.
 *
 * <p>User status policy:
 * <ul>
 *   <li>{@code INACTIVE} → {@link DisabledException} → translated to 401.</li>
 *   <li>{@code BLOCKED}  → {@link LockedException}   → translated to 401/403.</li>
 *   <li>{@code ACTIVE}   → authentication proceeds.</li>
 * </ul>
 *
 * <p>S3 update: returns a {@link BankUserPrincipal} so the {@code SecurityContext}
 * carries {@code userId}, {@code role}, and {@code relatedClientId}. The latter
 * is what {@link AuthorizationService} consults to enforce ownership filtering
 * from {@code @PreAuthorize} expressions, with no DB lookup per request.
 */
@Service
public class BankUserDetailsService implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserJpaRepository userJpaRepository;

    public BankUserDetailsService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (entity.getUserStatus() == UserStatus.INACTIVE) {
            throw new DisabledException("User account is inactive");
        }
        if (entity.getUserStatus() == UserStatus.BLOCKED) {
            throw new LockedException("User account is blocked");
        }

        // The role name maps 1:1 to a Spring authority prefixed with ROLE_,
        // e.g. INTERNAL_ANALYST -> ROLE_INTERNAL_ANALYST. This matches the
        // hasRole(...) checks in the @PreAuthorize matrix.
        String roleName = entity.getSystemRole().name();
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(ROLE_PREFIX + roleName));

        return new BankUserPrincipal(
                entity.getUserId(),
                entity.getUsername(),
                entity.getPassword(),
                roleName,
                entity.getRelatedClientId(),
                authorities
        );
    }
}
