package co.edu.tdea.bank.infrastructure.adapter.sql.repository;

import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BusinessClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BusinessClientJpaRepository extends JpaRepository<BusinessClientEntity, UUID> {
}
