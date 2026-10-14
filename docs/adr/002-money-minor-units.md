# ADR 002 — Money as integer minor units + ISO 4217 currency

**Status:** Accepted

## Context

MoneyMacros computes net pay, targets, safe-to-spend and projections. Binary
floating point (`double`) cannot represent decimal money exactly and leaks
rounding errors into headline numbers that the product promises to show
transparently ("show the math"). We need exact storage, precise calculation and
rounding rules that differ by country and year (tax brackets, contributions).

## Decision

- **Store** money as two columns: `amount_minor BIGINT` (the amount in minor
  units, e.g. centimes) and `currency CHAR(3)` (ISO 4217 code, e.g. `MAD`).
- **Calculate** with `BigDecimal`, never `double`/`float`.
- **Round** only where a rule says to; each country rule pack declares its own
  rounding mode and scale (see [ADR 004](004-rule-packs-versioned.md)), so the
  rounding applied to a number is explicit and auditable.
- A `shared` module provides `Money` and `Currency` value types wrapping these
  conventions.

## Consequences

- Exact storage and comparison; no floating-point drift in stored balances.
- Every amount carries its currency, which keeps future multi-currency work
  honest even though the MVP is MAD only.
- Calculations are verbose (`BigDecimal`) but correct and testable.
- Rounding is a declared, reviewable part of each rule pack rather than an
  accident of the type system, which supports the "honest numbers" guardrail.
