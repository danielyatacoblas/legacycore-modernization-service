ALTER SESSION SET CONTAINER=FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA=LEGACY;

BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE legacy_customers (
        customer_id NUMBER(19) PRIMARY KEY,
        full_name VARCHAR2(160 CHAR) NOT NULL,
        email VARCHAR2(254 CHAR) NOT NULL,
        updated_at TIMESTAMP NOT NULL
    )';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

MERGE INTO legacy_customers target
USING (
    SELECT 1001 customer_id, 'Ana Torres' full_name, 'ana.torres@example.com' email,
           TIMESTAMP '2026-08-01 10:00:00' updated_at FROM dual
    UNION ALL SELECT 1002, 'Luis Rojas', 'luis.rojas@example.com', TIMESTAMP '2026-08-02 11:30:00' FROM dual
    UNION ALL SELECT 1003, 'María Salazar', 'maria.salazar@example.com', TIMESTAMP '2026-08-03 09:15:00' FROM dual
) source
ON (target.customer_id = source.customer_id)
WHEN MATCHED THEN UPDATE SET target.full_name=source.full_name, target.email=source.email, target.updated_at=source.updated_at
WHEN NOT MATCHED THEN INSERT(customer_id, full_name, email, updated_at)
VALUES(source.customer_id, source.full_name, source.email, source.updated_at);

COMMIT;
