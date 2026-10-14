# ADR 004 — Country rule packs versioned by year

**Status:** Accepted

## Context

The career simulator converts gross salary into net take-home using a country's
tax and social-contribution rules (for Morocco: IR brackets, CNSS, AMO). These
rules change yearly and differ by country. A comparison made against 2026 rules
must stay reproducible even after 2027 rules land, and the product must show
which rule set produced a number ("honest numbers", "show the math").

## Decision

Model each country-and-year's rules as a **rule pack** identified by country and
year, e.g. `morocco-2026`. A rule pack declares its brackets, contribution rates,
caps and its rounding mode/scale (see [ADR 002](002-money-minor-units.md)). Job
offers and comparisons record the rule pack they were computed with. **Morocco
2026 is the first and only rule pack in the MVP**; France and others come later.

## Consequences

- Results are reproducible and auditable: a stored offer always knows its rules.
- Adding a country or a new tax year is additive — ship a new rule pack, do not
  edit old ones.
- The UI can name the rule pack behind every estimate, satisfying the "estimates
  are labelled with their assumptions" guardrail.
- Rule packs need their own tests and a clear authoring format; this is accepted
  complexity for correctness.
