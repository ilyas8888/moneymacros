# Architecture Decision Records

Each ADR captures one decision: its context, the decision, and its
consequences. See [ARCHITECTURE.md](../ARCHITECTURE.md) for the system overview.

| #   | Title | Status |
|-----|-------|--------|
| 001 | [Modular monolith with Spring Modulith, not microservices](001-modular-monolith.md) | Accepted |
| 002 | [Money as integer minor units + ISO 4217 currency](002-money-minor-units.md) | Accepted |
| 003 | [Server-side sessions in HttpOnly cookies + CSRF token](003-server-side-sessions.md) | Accepted |
| 004 | [Country rule packs versioned by year, Morocco 2026 first](004-rule-packs-versioned.md) | Accepted |
| 005 | [OpenAPI as the contract; TypeScript types generated from it](005-openapi-contract.md) | Accepted |
| 006 | [PostgreSQL only for now: no Redis, no message broker](006-postgres-only.md) | Accepted |
| 007 | [Module events are handled synchronously](007-synchronous-module-events.md) | Accepted |
| 008 | [One error format: RFC 9457 Problem Details](008-problem-details-errors.md) | Accepted |
