package com.portfolio.legacycore.migration;

import com.portfolio.legacycore.migration.MigrationModels.MigrationRequest;
import com.portfolio.legacycore.migration.MigrationModels.MigrationResult;
import com.portfolio.legacycore.migration.MigrationModels.MigrationStatus;
import com.portfolio.legacycore.migration.MigrationModels.ReconciliationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerMigrationService {
    private static final Instant INITIAL_WATERMARK = Instant.parse("1970-01-01T00:00:00Z");
    private final JdbcTemplate sourceJdbc;
    private final JdbcTemplate targetJdbc;
    private final TransactionTemplate transactionTemplate;
    private final ChecksumService checksumService;

    public CustomerMigrationService(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbc,
                                    JdbcTemplate targetJdbc,
                                    TransactionTemplate transactionTemplate,
                                    ChecksumService checksumService) {
        this.sourceJdbc = sourceJdbc;
        this.targetJdbc = targetJdbc;
        this.transactionTemplate = transactionTemplate;
        this.checksumService = checksumService;
    }

    public MigrationResult migrate(MigrationRequest request) {
        var startedAt = Instant.now();
        var runId = UUID.randomUUID();
        targetJdbc.update("INSERT INTO migration_run(id, status, started_at) VALUES (?, 'RUNNING', ?)",
                runId, Timestamp.from(startedAt));
        long migrated = 0;
        int batches = 0;
        boolean caughtUp = false;
        try {
            for (int index = 0; index < request.effectiveMaxBatches(); index++) {
                var checkpoint = checkpoint();
                var rows = readBatch(checkpoint.lastUpdatedAt(), checkpoint.lastId(), request.effectiveBatchSize());
                if (rows.isEmpty()) {
                    caughtUp = true;
                    break;
                }
                writeBatch(rows);
                migrated += rows.size();
                batches++;
                if (rows.size() < request.effectiveBatchSize()) {
                    caughtUp = true;
                    break;
                }
            }
            var finishedAt = Instant.now();
            targetJdbc.update("UPDATE migration_run SET status='COMPLETED', batches=?, migrated_rows=?, caught_up=?, finished_at=? WHERE id=?",
                    batches, migrated, caughtUp, Timestamp.from(finishedAt), runId);
            return new MigrationResult(runId, "COMPLETED", batches, migrated, caughtUp, startedAt, finishedAt);
        } catch (RuntimeException exception) {
            targetJdbc.update("UPDATE migration_run SET status='FAILED', error_message=?, finished_at=? WHERE id=?",
                    abbreviate(exception.getMessage()), Timestamp.from(Instant.now()), runId);
            throw exception;
        }
    }

    public MigrationStatus status() {
        var checkpoint = checkpoint();
        return targetJdbc.queryForObject("""
                SELECT ?, ?, COUNT(*) FILTER (WHERE status='COMPLETED'),
                       COUNT(*) FILTER (WHERE status='FAILED'), MAX(started_at)
                  FROM migration_run
                """, (rs, row) -> new MigrationStatus(checkpoint.lastUpdatedAt(), checkpoint.lastId(),
                rs.getLong(3), rs.getLong(4), instant(rs.getTimestamp(5))),
                Timestamp.from(checkpoint.lastUpdatedAt()), checkpoint.lastId());
    }

    public ReconciliationResult reconcile() {
        var sourceRows = sourceJdbc.query("SELECT customer_id, full_name, email, updated_at FROM legacy_customers ORDER BY customer_id",
                (rs, row) -> mapCustomer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4)));
        var targetRows = targetJdbc.query("SELECT legacy_id, full_name, email, source_updated_at FROM modern_customer ORDER BY legacy_id",
                (rs, row) -> mapCustomer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4)));
        var sourceHash = checksumService.checksum(sourceRows);
        var targetHash = checksumService.checksum(targetRows);
        return new ReconciliationResult(sourceRows.size(), targetRows.size(), sourceHash, targetHash,
                sourceRows.size() == targetRows.size() && sourceHash.equals(targetHash), Instant.now());
    }

    private List<LegacyCustomer> readBatch(Instant lastUpdatedAt, long lastId, int batchSize) {
        return sourceJdbc.query("""
                SELECT customer_id, full_name, email, updated_at
                  FROM legacy_customers
                 WHERE updated_at > ? OR (updated_at = ? AND customer_id > ?)
                 ORDER BY updated_at, customer_id
                 FETCH FIRST ? ROWS ONLY
                """, (rs, row) -> mapCustomer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4)),
                Timestamp.from(lastUpdatedAt), Timestamp.from(lastUpdatedAt), lastId, batchSize);
    }

    private void writeBatch(List<LegacyCustomer> rows) {
        transactionTemplate.executeWithoutResult(status -> {
            targetJdbc.batchUpdate("""
                    INSERT INTO modern_customer(legacy_id, full_name, email, source_updated_at, migrated_at)
                    VALUES (?, ?, ?, ?, now())
                    ON CONFLICT (legacy_id) DO UPDATE SET
                      full_name=EXCLUDED.full_name, email=EXCLUDED.email,
                      source_updated_at=EXCLUDED.source_updated_at, migrated_at=now()
                    WHERE modern_customer.source_updated_at <= EXCLUDED.source_updated_at
                    """, rows, rows.size(), (statement, row) -> {
                statement.setLong(1, row.id());
                statement.setString(2, row.fullName());
                statement.setString(3, row.email());
                statement.setTimestamp(4, Timestamp.from(row.updatedAt()));
            });
            var last = rows.getLast();
            targetJdbc.update("UPDATE migration_checkpoint SET last_updated_at=?, last_id=?, updated_at=now() WHERE stream_name='customers'",
                    Timestamp.from(last.updatedAt()), last.id());
        });
    }

    private Checkpoint checkpoint() {
        return targetJdbc.queryForObject("SELECT last_updated_at, last_id FROM migration_checkpoint WHERE stream_name='customers'",
                (rs, row) -> new Checkpoint(rs.getTimestamp(1).toInstant(), rs.getLong(2)));
    }

    private static LegacyCustomer mapCustomer(long id, String name, String email, Timestamp updatedAt) {
        return new LegacyCustomer(id, name, email, updatedAt.toInstant());
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String abbreviate(String message) {
        if (message == null) return "Unexpected migration error";
        return message.substring(0, Math.min(message.length(), 1_000));
    }

    private record Checkpoint(Instant lastUpdatedAt, long lastId) {
        private Checkpoint {
            if (lastUpdatedAt == null) lastUpdatedAt = INITIAL_WATERMARK;
        }
    }
}
