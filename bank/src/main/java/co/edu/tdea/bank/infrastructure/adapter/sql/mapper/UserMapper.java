package co.edu.tdea.bank.infrastructure.adapter.sql.mapper;

import co.edu.tdea.bank.domain.models.User;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static utility — converts {@link User} domain objects to/from JPA entities.
 *
 * <p>{@code UserEntity} now carries the full personal profile of the user
 * (Option A: the User aggregate owns its own profile data). The mapper performs
 * a 1:1 field translation; no placeholders or fallbacks are used.
 *
 * <p><b>Security fields (username/password):</b> these live on {@code UserEntity}
 * but are NOT part of the domain {@link User} model — they belong to the
 * security/authentication adapter. {@code toEntity(...)} therefore leaves them
 * untouched on creation; the security layer is responsible for populating them
 * before persistence on the auth path.
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
        entity.setFullName(user.getFullName());
        entity.setIdentificationId(user.getIdentificationId());
        entity.setEmail(user.getEmail());
        entity.setPhone(user.getPhone());
        entity.setBirthDate(user.getBirthDate());
        entity.setAddress(user.getAddress());
        // username / password are credentials — managed by the security adapter,
        // not by the domain User model. The auth path populates them separately
        // before invoking save() on the persistence port.
        return entity;
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .userId(entity.getUserId())
                .relatedClientId(entity.getRelatedClientId())
                .fullName(entity.getFullName())
                .identificationId(entity.getIdentificationId())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .birthDate(entity.getBirthDate())
                .address(entity.getAddress())
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
