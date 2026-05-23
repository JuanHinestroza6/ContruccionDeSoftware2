package co.edu.tdea.bank.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Bank-specific {@link UserDetails} implementation carried inside the
 * {@code SecurityContext}.
 *
 * <p>Beyond the standard credential/authority contract, this principal also
 * exposes:
 * <ul>
 *   <li>{@code userId} — the technical identifier of the {@code UserEntity}.</li>
 *   <li>{@code role} — the {@code SystemRole} name, without the {@code ROLE_}
 *       prefix that lives in {@link #getAuthorities()}.</li>
 *   <li>{@code relatedClientId} — the {@code Client} (individual or business)
 *       this user is bound to, or {@code null} for internal staff. This field is
 *       the cornerstone of ownership filtering enforced by
 *       {@link AuthorizationService} via SpEL in {@code @PreAuthorize}.</li>
 * </ul>
 *
 * <p>Embedding {@code relatedClientId} in the principal lets
 * {@code AuthorizationService} answer ownership questions without a database
 * round-trip per request. The trade-off is that a user must re-authenticate to
 * pick up a change to their {@code relatedClientId} — acceptable because that
 * field is set at user creation and not expected to change during a session.
 *
 * <p>All accounts in {@code Estado_Usuario = ACTIVE} are mapped here with
 * non-expired, non-locked, enabled flags; the {@code INACTIVE}/{@code BLOCKED}
 * gate is enforced earlier in {@link BankUserDetailsService}, so by the time we
 * build a principal we know the account is good to go.
 */
public final class BankUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String password;
    private final String role;
    private final UUID relatedClientId;
    private final Collection<? extends GrantedAuthority> authorities;

    public BankUserPrincipal(UUID userId,
                             String username,
                             String password,
                             String role,
                             UUID relatedClientId,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId          = Objects.requireNonNull(userId, "userId must not be null");
        this.username        = Objects.requireNonNull(username, "username must not be null");
        this.password        = Objects.requireNonNull(password, "password must not be null");
        this.role            = Objects.requireNonNull(role, "role must not be null");
        this.relatedClientId = relatedClientId;
        this.authorities     = Objects.requireNonNull(authorities, "authorities must not be null");
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    /**
     * May be {@code null} for internal staff (analysts, tellers, commercial
     * employees) that do not represent any single {@code Client}.
     */
    public UUID getRelatedClientId() {
        return relatedClientId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
