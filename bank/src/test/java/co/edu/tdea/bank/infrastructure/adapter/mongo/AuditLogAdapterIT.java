package co.edu.tdea.bank.infrastructure.adapter.mongo;

import co.edu.tdea.bank.domain.ports.out.AuditLogPort.AuditEntry;
import co.edu.tdea.bank.infrastructure.adapter.mongo.document.AuditLogDocument;
import co.edu.tdea.bank.infrastructure.adapter.mongo.repository.AuditLogMongoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AuditLogAdapter} backed by a real MongoDB
 * instance running on {@code localhost:27017}. Verifies persistence into the
 * {@code audit_logs} collection and the chronological ordering enforced by
 * the repository methods.
 */
@DataMongoTest
@ActiveProfiles("test")
@Import(AuditLogAdapter.class)
class AuditLogAdapterIT {

    private final AuditLogAdapter adapter;
    private final AuditLogMongoRepository mongo;
    private final MongoTemplate mongoTemplate;

    @Autowired
    AuditLogAdapterIT(AuditLogMongoRepository mongo, MongoTemplate mongoTemplate) {
        this.adapter = new AuditLogAdapter(mongo);
        this.mongo = mongo;
        this.mongoTemplate = mongoTemplate;
    }

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection(AuditLogDocument.class);
    }

    @Test
    void should_PersistAuditEntry_when_SavingValidEntry() {
        // Arrange
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 3, 10, 0);

        // Act
        adapter.save("Loan", "1001", "APPROVED",
                "user-123", occurredAt, "approved by analyst");

        // Assert
        List<AuditLogDocument> all = mongo.findAll();
        assertThat(all).hasSize(1);
        AuditLogDocument saved = all.get(0);
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getEntityType()).isEqualTo("Loan");
        assertThat(saved.getEntityId()).isEqualTo("1001");
        assertThat(saved.getAction()).isEqualTo("APPROVED");
        assertThat(saved.getPerformedBy()).isEqualTo("user-123");
        assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(saved.getDetail()).isEqualTo("approved by analyst");
    }

    @Test
    void should_PersistEntryWithNullDetail_when_DetailIsNotProvided() {
        // Arrange
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 3, 12, 0);

        // Act
        adapter.save("Transfer", "T-99", "EXPIRED",
                "system", occurredAt, null);

        // Assert
        List<AuditLogDocument> all = mongo.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getDetail()).isNull();
    }

    @Test
    void should_ReturnEntriesOrderedByOccurredAtAscending_when_FindByEntity() {
        // Arrange — insert out of chronological order on purpose
        adapter.save("Loan", "555", "DISBURSED",
                "user-A", LocalDateTime.of(2026, 5, 3, 12, 0), "second");
        adapter.save("Loan", "555", "APPROVED",
                "user-A", LocalDateTime.of(2026, 5, 3, 10, 0), "first");
        adapter.save("Loan", "555", "EXECUTED",
                "user-B", LocalDateTime.of(2026, 5, 3, 14, 0), "third");

        // Act
        List<AuditEntry> entries = adapter.findByEntity("Loan", "555");

        // Assert
        assertThat(entries).hasSize(3);
        assertThat(entries).extracting(AuditEntry::action)
                .containsExactly("APPROVED", "DISBURSED", "EXECUTED");
        assertThat(entries).extracting(AuditEntry::occurredAt)
                .isSorted();
    }

    @Test
    void should_ReturnOnlyEntriesForRequestedEntity_when_FindByEntity() {
        // Arrange
        LocalDateTime now = LocalDateTime.of(2026, 5, 3, 10, 0);
        adapter.save("Loan", "111", "APPROVED", "user", now, null);
        adapter.save("Loan", "222", "APPROVED", "user", now, null);
        adapter.save("Transfer", "111", "EXECUTED", "user", now, null);

        // Act
        List<AuditEntry> entries = adapter.findByEntity("Loan", "111");

        // Assert
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).entityType()).isEqualTo("Loan");
        assertThat(entries.get(0).entityId()).isEqualTo("111");
    }

    @Test
    void should_ReturnEmptyList_when_FindByEntityHasNoMatches() {
        // Act
        List<AuditEntry> entries = adapter.findByEntity("Loan", "DOES-NOT-EXIST");

        // Assert
        assertThat(entries).isEmpty();
    }

    @Test
    void should_ReturnEntriesOrderedByOccurredAtAscending_when_FindByPerformedBy() {
        // Arrange
        adapter.save("Loan", "1", "APPROVED",
                "alice", LocalDateTime.of(2026, 5, 3, 14, 0), "later");
        adapter.save("Transfer", "2", "EXECUTED",
                "alice", LocalDateTime.of(2026, 5, 3, 10, 0), "earlier");
        adapter.save("Loan", "3", "APPROVED",
                "bob", LocalDateTime.of(2026, 5, 3, 11, 0), "other user");

        // Act
        List<AuditEntry> entries = adapter.findByPerformedBy("alice");

        // Assert
        assertThat(entries).hasSize(2);
        assertThat(entries).allSatisfy(e -> assertThat(e.performedBy()).isEqualTo("alice"));
        assertThat(entries).extracting(AuditEntry::occurredAt)
                .isSorted();
    }

    @Test
    void should_ReturnEmptyList_when_FindByPerformedByHasNoMatches() {
        // Act
        List<AuditEntry> entries = adapter.findByPerformedBy("nobody");

        // Assert
        assertThat(entries).isEmpty();
    }

    @Test
    void should_AppendNotOverwrite_when_SavingMultipleEntriesForSameEntity() {
        // Arrange
        LocalDateTime t1 = LocalDateTime.of(2026, 5, 3, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 5, 3, 10, 5);

        // Act
        adapter.save("Loan", "777", "APPROVED", "user", t1, "first");
        adapter.save("Loan", "777", "DISBURSED", "user", t2, "second");

        // Assert — audit log is append-only; both entries coexist
        List<AuditLogDocument> all = mongo.findAll();
        assertThat(all).hasSize(2);
    }
}
