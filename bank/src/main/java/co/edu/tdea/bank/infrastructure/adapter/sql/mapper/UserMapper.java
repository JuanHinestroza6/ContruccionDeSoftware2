package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.model.User;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts {@link User} domain objects to/from JPA entities.
 *
 * <p><b>WARNING — schema mismatch:</b> {@code UserEntity} stores
 * {@code username}/{@code password} but lacks the personal fields the domain
 * {@link User} carries ({@code fullName}, {@code identificationId}, {@code email},
 * {@code phone}, {@code birthDate}, {@code address}). Until the entity is
 * extended (or those fields are sourced from the linked {@link
 * co.edu.tdea.bank.domain.model.Client}), {@code toDomain(...)} fills the gaps
 * with derived placeholders so the {@link User.Builder} contract is honoured.
 * Treat the resulting domain object as auth/role-metadata only — the personal
 * fields are NOT authoritative until the schema is reconciled.
 */
public final class UserMapper {

    private UserMapper() {}

    public static UserEntity toEntity(User user) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        entity.setUserId(user.getUserId());
        entity.setRelatedClientId(user.getRelatedClientId());
        entity.setSystemRole(user.getSystemRole());
        entity.setUserStatus(user.getUserStatus());
        // username/password are managed outside the domain User model — adapter
        // callers must populate them before invoking save() through the auth path.
        return entity;
    }

    /**
     * Rehydrates a domain {@link User} using the columns currently available in
     * {@link UserEntity}. Personal fields are filled with derived placeholders
     * (see class-level WARNING). Replace with real data once the entity carries
     * those columns or once a join with {@code ClientEntity} is wired in.
     */
    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        String fallback = entity.getUsername() != null ? entity.getUsername() : entity.getUserId().toString();
        return User.builder()
                .userId(entity.getUserId())
                .relatedClientId(entity.getRelatedClientId())
                .fullName(fallback)
                .identificationId(fallback)
                .email(fallback.contains("@") ? fallback : fallback + "@placeholder.local")
                .phone("0000000000")
                .birthDate(LocalDate.of(1970, 1, 1))
                .address("UNKNOWN")
                .systemRole(entity.getSystemRole())
                .userStatus(entity.getUserStatus())
                .build();
    }

    public static List<User> toDomainList(List<UserEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(UserMapper::toDomain)
                .collect(Collectors.toList());
    }
}
