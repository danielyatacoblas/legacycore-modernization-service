package com.portfolio.legacycore.migration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

public final class MigrationModels {
    private MigrationModels() { }

    public record MigrationRequest(
            @Min(1) @Max(5_000) Integer batchSize,
            @Min(1) @Max(1_000) Integer maxBatches) {
        public int effectiveBatchSize() { return batchSize == null ? 250 : batchSize; }
        public int effectiveMaxBatches() { return maxBatches == null ? 100 : maxBatches; }
    }

    public record MigrationResult(UUID runId, String status, int batches, long migratedRows,
                                  boolean caughtUp, Instant startedAt, Instant finishedAt) { }

    public record MigrationStatus(Instant lastUpdatedAt, long lastId, long completedRuns,
                                  long failedRuns, Instant lastRunAt) { }

    public record ReconciliationResult(long sourceCount, long targetCount, String sourceChecksum,
                                       String targetChecksum, boolean consistent, Instant checkedAt) { }
}
