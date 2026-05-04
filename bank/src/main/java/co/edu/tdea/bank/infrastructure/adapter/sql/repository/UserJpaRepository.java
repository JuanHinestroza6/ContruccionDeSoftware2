package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.domain.enums.SystemRole;
import co.edu.tdea.bank.domain.enums.UserStatus;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByRelatedClientId(UUID relatedClientId);

    boolean existsByUsername(String username);

    List<UserEntity> findBySystemRole(SystemRole systemRole);

    List<UserEntity> findByUserStatus(UserStatus userStatus);
}
