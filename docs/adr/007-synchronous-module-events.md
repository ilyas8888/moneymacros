# ADR 007 — Module events are handled synchronously

**Status:** Accepted

## Context

Applying a job offer must update the `profile` (net income), the `planning`
targets and the `goals` projected dates before the API responds, and it must be
all-or-nothing: if any step fails, the whole action rolls back and the user sees
no half-applied plan. The work is a handful of in-memory recalculations that
take milliseconds, so there is no reason to defer it past the response.

## Decision

Handle module events **synchronously, inside the publisher's transaction**,
using plain Spring event listeners (`@EventListener` only) — **not**
`@ApplicationModuleListener`, which Spring Modulith runs asynchronously after
commit. Publishing an event and handling it are part of the same transaction as
the originating command. `@TransactionalEventListener` is not used: it runs after
the commit, so a listener's database writes would not be saved unless it starts a
new transaction.

## Consequences

- One simple consistency model: a command and all its event handlers commit or
  roll back together. A failing listener rolls back the whole action.
- The dashboard is up to date the moment the apply call returns; no eventual
  consistency to reason about.
- The publisher's transaction grows with each listener it fans out to, so a slow
  listener slows the whole request.
- When a listener becomes slow or is only secondary to the command (e.g. sending
  mail, heavy projections), move it to `@ApplicationModuleListener`: it then runs
  asynchronously after commit and is tracked by Spring Modulith's event
  publication registry (see [ADR 006](006-postgres-only.md)) for retry.
