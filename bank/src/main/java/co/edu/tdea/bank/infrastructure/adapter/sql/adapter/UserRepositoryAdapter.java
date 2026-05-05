package co.edu.tdea.bank.infrastructure.adapter.sql.adapter;

import co.edu.tdea.bank.application.port.out.UserRepositoryPort;
import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.domain.model.User;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.mapper.UserMapper;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpa.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return jpa.findById(userId).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByRelatedClientId(UUID clientId) {
        return jpa.findByRelatedClientId(clientId).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public List<User> findByRole(SystemRole role) {
        return UserMapper.toDomainList(jpa.findBySystemRole(role));
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        return UserMapper.toDomainList(jpa.findByUserStatus(status));
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }
}
