# MoneyMacros — Architecture

This document describes the system through a few complementary views: the C4
**context** and **container** views, the **API module map**, the **data model**
(ERD v1), and one **sequence** for the core flow. Diagrams use Mermaid so they
render on GitHub and in Obsidian. Start with [PRODUCT.md](PRODUCT.md) for scope,
[GLOSSARY.md](GLOSSARY.md) for the vocabulary; decisions live in
[adr/](adr/README.md).

> Mermaid's dedicated C4 syntax is still experimental, so the C4 views below are
> drawn as plain `flowchart` diagrams.

## C4 — System context

Who uses MoneyMacros and which external systems it talks to. The bank-data
aggregator and the email service are **after MVP** (dashed).

```mermaid
flowchart TB
    graduate["Young graduate<br/><i>Person</i><br/>Compares offers, plans a budget"]
    mm["MoneyMacros<br/><i>System</i><br/>Turns income and offers into a plan,<br/>safe-to-spend and goal dates"]

    aggregator["Bank-data aggregator<br/><i>External system — after MVP</i>"]
    email["Email service<br/><i>External system — after MVP</i>"]

    graduate --> mm
    mm -.->|"imports transactions<br/>(after MVP)"| aggregator
    mm -.->|"sends password-reset mail<br/>(after MVP)"| email

    classDef future stroke-dasharray: 5 5;
    class aggregator,email future;
```

## C4 — Containers

The MVP ships the web app, the API and the database. The Expo mobile app is
**after MVP** (dashed).

```mermaid
flowchart TB
    graduate["Young graduate<br/><i>Person</i>"]

    subgraph mm["MoneyMacros"]
        web["Web app<br/><i>React, Vite, TypeScript</i>"]
        api["API<br/><i>Java 25, Spring Boot 4,<br/>Spring Modulith</i>"]
        db[("PostgreSQL<br/><i>Database</i>")]
        mobile["Mobile app<br/><i>Expo — after MVP</i>"]
    end

    graduate -->|"uses (HTTPS)"| web
    web -->|"JSON over HTTPS<br/>session cookie + CSRF"| api
    api -->|"reads/writes (JDBC)"| db
    mobile -.->|"JSON over HTTPS (after MVP)"| api

    classDef future stroke-dasharray: 5 5;
    class mobile future;
```

## API — module map

The API is a modular monolith (Spring Modulith, [ADR 001](adr/001-modular-monolith.md)).
Each module owns its tables and talks to others **only through their public API
or their published events** — never by reaching into another's internals.

```mermaid
flowchart LR
    identity["identity"]
    profile["profile"]
    planning["planning"]
    ledger["ledger"]
    goals["goals"]
    career["career"]
    insights["insights"]
    shared["shared"]

    identity -->|UserRegistered| profile
    career -->|OfferApplied| profile
    profile -->|ProfileUpdated| planning
    planning -->|PlanRecalculated| goals
    career -.->|reads public API| profile
    career -.->|reads public API| planning
    career -.->|reads public API| goals
    insights -.->|reads public API| planning
    insights -.->|reads public API| ledger
    insights -.->|reads public API| goals
```

Every module may use `shared` (`Money`, `Currency`, error types) — not drawn.

| Module | Owns | Listens to | Publishes |
|--------|------|------------|-----------|
| **identity** | accounts, sessions, audit log | — | `UserRegistered` |
| **profile** | country, currency, city, household, net income | `UserRegistered`, `OfferApplied` | `ProfileUpdated` |
| **planning** | budget method, macro and category targets | `ProfileUpdated` | `PlanRecalculated` |
| **ledger** | expenses, categories, recipes | — | — |
| **goals** | goals, contributions (money set aside for a goal) and projected dates | `PlanRecalculated` | — |
| **career** | rule packs, job offers, comparisons, reverse mode | — | `OfferApplied` |
| **insights** | safe-to-spend and Budget-Score calculations; stores nothing | — | — |
| **shared** | `Money`, `Currency`, error types (no business logic) | — | — |

`career` reads `profile` (household size, for family tax deductions), `planning`
and `goals` through their public APIs to build comparisons and projections; it
changes the plan only by publishing `OfferApplied`. `insights` stores nothing and
listens to nothing: on a dashboard request it reads `planning`, `ledger` and
`goals` and computes safe-to-spend and the Budget-Score on the spot.

## Data model — ERD v1

Every user-owned table carries `user_id`. Money is `amount_minor BIGINT` plus
`currency CHAR(3)` (ISO 4217); see [ADR 002](adr/002-money-minor-units.md).

```mermaid
erDiagram
    users ||--o| profiles : has
    users ||--o{ budget_plans : owns
    budget_plans ||--o{ plan_targets : contains
    users ||--o{ categories : owns
    users ||--o{ transactions : logs
    categories ||--o{ transactions : classifies
    users ||--o{ recipes : owns
    categories ||--o{ recipes : classifies
    users ||--o{ goals : sets
    goals ||--o{ goal_contributions : receives
    users ||--o{ job_offers : enters
    job_offers ||--o{ offer_benefits : lists
    users ||--o{ audit_events : records

    users {
        uuid id PK
        string email
        string password_hash
        timestamptz created_at
    }
    profiles {
        uuid id PK
        uuid user_id FK
        char country
        char currency
        string city
        int household_size
        bigint net_income_minor
    }
    budget_plans {
        uuid id PK
        uuid user_id FK
        string method
        boolean active
    }
    plan_targets {
        uuid id PK
        uuid budget_plan_id FK
        string macro
        bigint amount_minor
        char currency
    }
    categories {
        uuid id PK
        uuid user_id FK
        string macro
        string name
    }
    transactions {
        uuid id PK
        uuid user_id FK
        uuid category_id FK
        bigint amount_minor
        char currency
        date spent_on
    }
    recipes {
        uuid id PK
        uuid user_id FK
        uuid category_id FK
        bigint amount_minor
        char currency
        string cadence
    }
    goals {
        uuid id PK
        uuid user_id FK
        string name
        bigint target_amount_minor
        char currency
        date target_date
        date projected_date
    }
    goal_contributions {
        uuid id PK
        uuid goal_id FK
        uuid user_id FK
        bigint amount_minor
        char currency
        date contributed_on
    }
    job_offers {
        uuid id PK
        uuid user_id FK
        string rule_pack
        bigint gross_amount_minor
        char currency
    }
    offer_benefits {
        uuid id PK
        uuid job_offer_id FK
        string kind
        bigint amount_minor
        char currency
    }
    audit_events {
        uuid id PK
        uuid user_id FK
        string action
        timestamptz occurred_at
    }
```

The session and event-publication tables are created by Spring Session and
Spring Modulith, so they are not drawn here.

## Sequence — apply an offer to my plan

From the click to the refreshed dashboard. Events fan out from `career` through
`profile`, `planning` and `goals`; `insights` is read on demand for the dashboard.

```mermaid
sequenceDiagram
    actor U as Graduate
    participant W as Web app
    participant C as career
    participant P as profile
    participant PL as planning
    participant G as goals
    participant I as insights

    U->>W: Click "Apply this offer"
    W->>C: POST /offers/{id}/apply (cookie + CSRF)
    C->>C: Resolve net take-home from rule pack
    C-->>P: OfferApplied
    P->>P: Update net income
    P-->>PL: ProfileUpdated
    PL->>PL: Recalculate macro + category targets
    PL-->>G: PlanRecalculated
    G->>G: Recompute projected goal dates
    Note over C,G: Events run synchronously in career's transaction (ADR 007)
    C-->>W: 200 OK
    W->>I: GET /dashboard
    Note over W,I: insights reads planning, ledger and goals via their public APIs
    I-->>W: Safe-to-spend, rings, Budget-Score, goal dates
    W-->>U: Refreshed dashboard
```
