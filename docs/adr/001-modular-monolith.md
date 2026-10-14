# ADR 001 — Modular monolith with Spring Modulith, not microservices

**Status:** Accepted

## Context

MoneyMacros is a solo-built MVP due 10 Nov 2026 with a small, clear set of
domains (identity, profile, planning, ledger, goals, career, insights). The
domains have strong boundaries but share one user and one dataset. Microservices
would add network calls, distributed transactions, multiple deploy units and
operational overhead that a one-person MVP cannot justify, while still needing
the same domain boundaries drawn correctly.

## Decision

Build a single Spring Boot application split into modules enforced by
[Spring Modulith](https://spring.io/projects/spring-modulith). Each module owns
its tables and exposes a narrow public API; modules communicate only through
that public API or through published application events. Module boundaries are
verified in tests with Spring Modulith's `ApplicationModules` checks.

## Consequences

- One build, one deploy unit, one database connection — simple to run and host.
- Boundaries are explicit and test-enforced, so the code stays decomposable if a
  module ever needs to become a service later.
- In-process events keep coupling loose without a broker (see
  [ADR 006](006-postgres-only.md)).
- A single process is a single scaling and failure unit; acceptable at MVP
  scale.
- Developers must respect module APIs; reaching into another module's internals
  fails the architecture tests.
