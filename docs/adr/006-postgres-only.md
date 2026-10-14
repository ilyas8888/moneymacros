# ADR 006 — PostgreSQL only for now: no Redis, no message broker

**Status:** Accepted

## Context

The modular monolith (see [ADR 001](001-modular-monolith.md)) uses application
events between modules, needs a session store (see
[ADR 003](003-server-side-sessions.md)), and persists all domain data. It is
tempting to reach for Redis for sessions/caching and a broker (Kafka/RabbitMQ)
for events. Each adds infrastructure to run, monitor and pay for — a real cost
for a solo MVP that must ship by 10 Nov 2026.

## Decision

Use **PostgreSQL as the only datastore** for the MVP:

- Domain data in PostgreSQL tables.
- Sessions via Spring Session, backed by PostgreSQL.
- In-process application events, synchronous for now (see
  [ADR 007](007-synchronous-module-events.md)); Spring Modulith's
  event-publication log in PostgreSQL matters once listeners become
  asynchronous.

No Redis, no message broker.

## Consequences

- One piece of infrastructure to provision, back up and host — minimal ops.
- Events are in-process and transactional with the database; simple and
  consistent at MVP scale.
- No cross-process pub/sub or distributed cache; if load or multi-instance needs
  arrive later, introducing Redis or a broker is a deliberate future ADR.
- All durability and availability concerns concentrate on one PostgreSQL
  instance, which must be backed up properly.
