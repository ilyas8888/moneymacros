# ADR 003 — Server-side sessions in HttpOnly cookies + CSRF token

**Status:** Accepted

## Context

The web app needs authenticated requests to the API. A common SPA pattern stores
a JWT in `localStorage` or `sessionStorage`, but anything JavaScript can read, an
XSS bug can steal, and browser-stored tokens cannot be revoked server-side before
they expire. MoneyMacros holds personal financial data and values privacy, so
session theft via XSS is the risk to design against.

## Decision

- Use **server-side sessions**: the API creates a session, persisted by Spring
  Session, and returns the session ID in an **HttpOnly, Secure, SameSite**
  cookie. JavaScript cannot read it, so an XSS payload cannot exfiltrate it.
- Protect state-changing requests with a **CSRF token** (double-submit / header
  pattern) that the SPA reads and echoes, since cookies are sent automatically.
- **No JWT in browser storage.**

## Consequences

- Sessions are revocable server-side (logout, forced expiry) — not possible with
  self-contained browser JWTs.
- The browser never handles the session secret directly.
- The API is stateful in the session store; backed by PostgreSQL via Spring
  Session (see [ADR 006](006-postgres-only.md)), no extra infrastructure.
- The SPA must send the CSRF token on writes, and the API must enforce it.
- The future Expo app will need its own token strategy; this ADR governs the
  browser only.
