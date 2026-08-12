# LegacyCore Modernization Service

Servicio Java 21 + Spring Boot para una modernización incremental Oracle → PostgreSQL con reanudación segura, trazabilidad y reconciliación verificable.

## Capacidades

- Lectura incremental mediante watermark compuesto `(updated_at, customer_id)`.
- Upsert idempotente y checkpoint en una única transacción PostgreSQL.
- Historial de ejecuciones completadas/fallidas y límites de protección por lote.
- Reconciliación por cantidad y SHA-256 canónico de todos los registros.
- APIs REST con validación y errores RFC 9457 (`ProblemDetail`).
- Health checks, Prometheus, Docker, CI y unidad `systemd` para Linux.

## Ejecución local

Requiere Java 21 y Docker Desktop.

```bash
docker compose up -d --wait
./gradlew bootRun
```

En otra consola:

```bash
curl -X POST http://localhost:8095/api/v1/migrations/customers \
  -H 'Content-Type: application/json' \
  -d '{"batchSize":2,"maxBatches":10}'
curl http://localhost:8095/api/v1/migrations/customers/status
curl http://localhost:8095/api/v1/reconciliation/customers
```

La reconciliación esperada contiene `"consistent":true`. Oracle se inicializa con tres clientes demostrativos.

## API

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/v1/migrations/customers` | Ejecuta hasta `maxBatches` lotes |
| GET | `/api/v1/migrations/customers/status` | Watermark y ejecuciones |
| GET | `/api/v1/reconciliation/customers` | Conteos y checksums |
| GET | `/actuator/health` | Salud operativa |
| GET | `/actuator/prometheus` | Métricas |

Consulta [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) para decisiones y garantías de recuperación.
