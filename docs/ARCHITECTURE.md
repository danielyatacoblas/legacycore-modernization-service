# Arquitectura

Oracle permanece como sistema legado de lectura y PostgreSQL es el destino moderno. El migrador usa una marca de agua compuesta `(updated_at, customer_id)`, por lo que no pierde filas que comparten timestamp. Cada lote ejecuta sus `upsert` y el avance del checkpoint dentro de la misma transacción PostgreSQL.

```text
Oracle legacy_customers
        │ lectura ordenada y paginada
        ▼
CustomerMigrationService ── transacción ──► modern_customer
        │                                      + checkpoint
        └──────────────────────────────────► migration_run

Reconciliación: COUNT + SHA-256 canónico de origen y destino
```

Si el proceso cae antes del commit, el checkpoint no avanza. Al reintentar, `ON CONFLICT` hace segura la repetición del lote. El endpoint limita tamaño y cantidad de lotes para proteger ambos motores.
