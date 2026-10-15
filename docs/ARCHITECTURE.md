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

    classDef person fill:#08427B,color:#ffffff;
    classDef system fill:#1168BD,color:#ffffff;
    classDef external fill:#999999,color:#ffffff,stroke-dasharray:5 5;
    class graduate person;
    class mm system;
    class aggregator,email external;
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

    classDef person fill:#08427B,color:#ffffff;
    classDef container fill:#438DD5,color:#ffffff;
    classDef future fill:#999999,color:#ffffff,stroke-dasharray:5 5;
    class graduate person;
    class web,api,db container;
    class mobile future;
```

## API — module map

The API is a modular monolith (Spring Modulith, [ADR 001](adr/001-modular-monolith.md)).
Each module owns its tables and talks to others **only through their public API
or their published events** — never by reaching into another's internals.

> [!IMPORTANT]
> A module may touch another only through its public API or the events it
> publishes. No cross-module table access, no shared internals — this is what
> keeps the monolith decomposable.

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

    style identity fill:#E9D5FF,stroke:#7C3AED,color:#111827
    style profile fill:#BFDBFE,stroke:#2563EB,color:#111827
    style planning fill:#BBF7D0,stroke:#16A34A,color:#111827
    style ledger fill:#FED7AA,stroke:#EA580C,color:#111827
    style goals fill:#FEF08A,stroke:#CA8A04,color:#111827
    style career fill:#FECDD3,stroke:#E11D48,color:#111827
    style insights fill:#A5F3FC,stroke:#0891B2,color:#111827
    style shared fill:#E5E7EB,stroke:#6B7280,color:#111827
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

> [!WARNING]
> Money is never a `double`. Store integer minor units + an ISO 4217 currency,
> and compute with `BigDecimal`. Floating point silently loses precision on
> money.

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

Spring Session's tables come from the library, not from a module, so they are
not drawn here.

## Sequence — apply an offer to my plan

From the click to the refreshed dashboard. Events fan out from `career` through
`profile`, `planning` and `goals`; `insights` is read on demand for the dashboard.

> [!IMPORTANT]
> Module events are handled **synchronously, in the publisher's transaction**
> (see [ADR 007](adr/007-synchronous-module-events.md)). The whole fan-out
> commits or rolls back together, so the dashboard is current the moment the
> apply call returns.

```mermaid
sequenceDiagram
    actor U as Graduate
    participant W as Web app
    box rgba(225,29,72,0.18) career
    participant C as career
    end
    box rgba(37,99,235,0.18) profile
    participant P as profile
    end
    box rgba(22,163,74,0.18) planning
    participant PL as planning
    end
    box rgba(202,138,4,0.18) goals
    participant G as goals
    end
    box rgba(8,145,178,0.18) insights
    participant I as insights
    end

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
