package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByIdentificationId(String identificationId);

    Optional<ClientEntity> findByEmail(String email);

    boolean existsByIdentificationId(String identificationId);

    boolean existsByEmail(String email);
}
