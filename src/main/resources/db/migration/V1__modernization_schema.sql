CREATE TABLE modern_customer (
    legacy_id BIGINT PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    source_updated_at TIMESTAMPTZ NOT NULL,
    migrated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_modern_customer_email ON modern_customer (lower(email));

CREATE TABLE migration_checkpoint (
    stream_name VARCHAR(80) PRIMARY KEY,
    last_updated_at TIMESTAMPTZ NOT NULL,
    last_id BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO migration_checkpoint(stream_name, last_updated_at, last_id)
VALUES ('customers', '1970-01-01 00:00:00+00', 0);

CREATE TABLE migration_run (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING','COMPLETED','FAILED')),
    batches INTEGER NOT NULL DEFAULT 0,
    migrated_rows BIGINT NOT NULL DEFAULT 0,
    caught_up BOOLEAN NOT NULL DEFAULT false,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ
);

CREATE INDEX idx_migration_run_started ON migration_run(started_at DESC);
