package co.edu.tdea.bank.infrastructure.adapter.sql.adapter;

import co.edu.tdea.bank.domain.ports.out.ClientRepositoryPort;
import co.edu.tdea.bank.domain.models.BusinessClient;
import co.edu.tdea.bank.domain.models.Client;
import co.edu.tdea.bank.domain.models.IndividualClient;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.BusinessClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.ClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.entity.IndividualClientEntity;
import co.edu.tdea.bank.infrastructure.adapter.sql.mapper.ClientMapper;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.BusinessClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.ClientJpaRepository;
import co.edu.tdea.bank.infrastructure.adapter.sql.repository.IndividualClientJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClientRepositoryAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository clientJpa;
    private final IndividualClientJpaRepository individualJpa;
    private final BusinessClientJpaRepository businessJpa;

    public ClientRepositoryAdapter(ClientJpaRepository clientJpa,
                                   IndividualClientJpaRepository individualJpa,
                                   BusinessClientJpaRepository businessJpa) {
        this.clientJpa     = clientJpa;
        this.individualJpa = individualJpa;
        this.businessJpa   = businessJpa;
    }

    @Override
    public Client save(Client client) {
        if (client instanceof IndividualClient ind) {
            IndividualClientEntity saved = individualJpa.save(ClientMapper.toEntity(ind));
            return ClientMapper.toDomain(saved);
        }
        if (client instanceof BusinessClient biz) {
            BusinessClientEntity saved = businessJpa.save(ClientMapper.toEntity(biz));
            return ClientMapper.toDomain(saved);
        }
        throw new IllegalArgumentException(
                "Unknown Client subtype: " + client.getClass().getName());
    }

    @Override
    public Optional<Client> findById(UUID clientId) {
        return clientJpa.findById(clientId).map(ClientMapper::toDomain);
    }

    @Override
    public Optional<Client> findByIdentificationId(String identificationId) {
        return clientJpa.findByIdentificationId(identificationId).map(ClientMapper::toDomain);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return clientJpa.findByEmail(email).map(ClientMapper::toDomain);
    }

    @Override
    public List<IndividualClient> findAllIndividualClients() {
        return ClientMapper.toIndividualDomainList(individualJpa.findAll());
    }

    @Override
    public List<BusinessClient> findAllBusinessClients() {
        return ClientMapper.toBusinessDomainList(businessJpa.findAll());
    }

    @Override
    public boolean existsByIdentificationId(String identificationId) {
        return clientJpa.existsByIdentificationId(identificationId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return clientJpa.existsByEmail(email);
    }
}
