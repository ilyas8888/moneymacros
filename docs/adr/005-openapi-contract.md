# ADR 005 — OpenAPI as the contract; TypeScript types generated from it

**Status:** Accepted

## Context

The React web app and the Spring Boot API are developed together but are
separate build units. Hand-written TypeScript request/response types drift from
the server over time, producing bugs that only surface at runtime. We want the
API shape to have one source of truth that both sides agree on.

## Decision

Treat the **OpenAPI specification as the contract** between web and API. The
Spring Boot API produces the OpenAPI document (springdoc), and the web app
**generates its TypeScript types from that document** as a build step. Client
code imports the generated types rather than re-declaring request/response
shapes by hand.

## Consequences

- One source of truth; a breaking API change surfaces as a TypeScript
  compile error in the web app, not a runtime failure.
- The spec doubles as live, always-current API documentation.
- Generation is a build dependency the web app must run when the API changes.
- The API must keep its OpenAPI output accurate (annotations, response types),
  which is good discipline anyway.
