---
paths: ["api/**"]
---

# Backend rules

- Modules are the direct sub-packages of `com.moneymacros`. A module uses another only through its public API (types in its top-level package) or the events it publishes, never its internal sub-packages or tables. `ModularityTests` must stay green. (ADR 001)
- Module events use plain `@EventListener`: synchronous, inside the publisher's transaction. No `@TransactionalEventListener`; `@ApplicationModuleListener` only for slow or secondary listeners. (ADR 007)
- Money is `BIGINT` minor units plus a `CHAR(3)` ISO 4217 currency in the database, and `BigDecimal` in calculations; never `double` or `float`. (ADR 002)
- Controllers live under `/api`, take and return DTO records (never JPA entities) and validate request bodies with `@Valid`. The OpenAPI contract is generated from them. (ADR 005)
- Schema changes go through Flyway only (`ddl-auto=validate`). Migrations are named `V<n>__<description>.sql`, starting at `V1`. Never edit a migration once it is pushed; add a new one.
- Every endpoint gets a `@WebMvcTest` slice test. Anything that touches the database runs on Testcontainers `postgres:18`, never H2. (ADR 006)
- Tax rules live in year-versioned rule packs, never as constants in services. (ADR 004)
- Auth uses server-side sessions in an HttpOnly cookie plus a CSRF token, never JWTs in browser storage. (ADR 003)
