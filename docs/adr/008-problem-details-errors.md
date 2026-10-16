# ADR 008 — One error format: RFC 9457 Problem Details

**Status:** Accepted

## Context

The API will return errors from many places: bean validation, malformed JSON,
unknown URLs, wrong HTTP methods, and later domain rules. If each of these
answers in its own shape, the web app needs a different parser per error and
users see inconsistent messages. We also must not leak internal details —
exception messages or stack traces — to clients, while still being able to
trace a failure back to a server log.

## Decision

- Every error is an **RFC 9457 Problem Detail** (`application/problem+json`),
  produced by a single global `@RestControllerAdvice` in
  `com.moneymacros.shared.web`. It extends Spring's
  `ResponseEntityExceptionHandler`, so Spring's own errors (malformed JSON,
  unknown URL, wrong method) are Problem Details too.
- **Validation errors (400)** add an `errors` array of `{field, message}` so the
  web app can show messages next to the right inputs.
- **Any other exception (500)** returns a generic title and detail — never the
  exception message or a stack trace. The exception is logged under a random
  `errorId`, and that same `errorId` is returned in the body so a user report
  maps to one log entry.
- Domain exceptions are not mapped yet; the endpoints that raise them come
  later. Spring Security's 401 and 403 are aligned on the same format on Day 5.

## Consequences

- The web app has one error shape to parse, with a richer `errors` list for
  validation.
- Internal details never reach clients, yet every 500 is traceable through its
  `errorId`, which supports the "honest, debuggable" guardrail.
- New error cases must be routed through this advice rather than handled ad hoc,
  so the format stays uniform.
