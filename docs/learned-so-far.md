# Learned so far: Days 1 to 7

A study log kept while building MoneyMacros, 13 to 20 October 2026. Each section explains one idea, shows where it lives in the project, then asks questions: click a question to reveal its answer (in Obsidian, switch to Reading view).

From Day 6, new ideas come as **defense cards**, built to hold your ground in front of a jury or an interviewer:

- **The picture:** a diagram you could redraw on a whiteboard.
- **One sentence:** the idea, without jargon.
- **For a non-developer:** an everyday comparison, because juries often include people who don't code.
- **In MoneyMacros:** where it lives in the code.
- **The ladder:** a jury digs in three steps (What is it? Why this choice? What if…?), so each card prepares all three.

> [!TIP]
> Revise like an interview: read a question, answer it out loud, then open it to compare.

## At a glance

| Day | What was built | Ideas to know |
|---|---|---|
| 1 · Tue 13 Oct | Product brief, glossary, rules for Claude Code | Money is never a `double`, HttpOnly cookies, monolith first |
| 2 · Wed 14 Oct | Architecture, module map, ADRs 001 to 007 | C4, ADRs, events vs reads, synchronous events |
| 3 · Thu 15 Oct | Walking skeleton: React → Spring Boot → PostgreSQL, CI | How a request travels, configuration, dependency injection |
| 4 · Fri 16 Oct | `Money` type, one error format, a base for database tests | `BigDecimal` scale, Problem Details, kinds of tests |
| 5 · Sat 17 Oct | Sign-up, log-in, log-out | Salted password hashing, sessions, CSRF, a port clash |
| 6 · Sun 18 Oct | Log-in pages, generated API types, French and English | The API contract, props and state, server state, failure-path testing, pull requests |
| 7 · Tue 20 Oct | Sign-up race fix, scoped error handlers, Money tests | Race conditions, controller advice, mocks versus a real database |

## 1. Architecture: one app, eight modules

**Modular monolith.** MoneyMacros is one Spring Boot application with one database, split into modules that talk only through public APIs or events. A Spring Modulith test (`ModularityTests`) fails the build if a module reaches into another one's internals. Microservices would add network calls, distributed transactions and several deployments, with nothing gained for a one-person project. A module can still be extracted later, because its boundary is clean.

**Two ways to talk.** Ask one question per arrow:

- **Event, A → B:** when A changes, must B's own data change? Then A announces an event, and B updates itself.
- **Read, A → B:** does A need B's numbers to do its job? Then A calls B's public API, and nothing changes.

Events are named in the past tense (`UserRegistered`): a fact that happened, not an order.

```mermaid
flowchart LR
    identity["identity<br/>accounts, sessions"] -->|UserRegistered| profile["profile<br/>income, city, household"]
    career["career<br/>job offers, rule packs"] -->|OfferApplied| profile
    profile -->|ProfileUpdated| planning["planning<br/>budget method, targets"]
    planning -->|PlanRecalculated| goals["goals<br/>goals, contributions, dates"]

    style identity fill:#E9D5FF,stroke:#7C3AED,color:#111827
    style profile fill:#BFDBFE,stroke:#2563EB,color:#111827
    style planning fill:#BBF7D0,stroke:#16A34A,color:#111827
    style goals fill:#FEF08A,stroke:#CA8A04,color:#111827
    style career fill:#FECDD3,stroke:#E11D48,color:#111827
```

Four events form one chain. identity starts it, and career joins it with `OfferApplied`.

Six reads: career and insights each ask three modules.

```mermaid
flowchart LR
    career["career<br/>job offers, rule packs"] -.->|reads| profile["profile<br/>income, city, household"]
    career -.->|reads| planning["planning<br/>budget method, targets"]
    career -.->|reads| goals["goals<br/>goals, contributions, dates"]

    style career fill:#FECDD3,stroke:#E11D48,color:#111827
    style profile fill:#BFDBFE,stroke:#2563EB,color:#111827
    style planning fill:#BBF7D0,stroke:#16A34A,color:#111827
    style goals fill:#FEF08A,stroke:#CA8A04,color:#111827
```

```mermaid
flowchart LR
    insights["insights<br/>safe-to-spend, Budget-Score"] -.->|reads| planning["planning<br/>budget method, targets"]
    insights -.->|reads| goals["goals<br/>goals, contributions, dates"]
    insights -.->|reads| ledger["ledger<br/>expenses, categories, recipes"]

    style insights fill:#A5F3FC,stroke:#0891B2,color:#111827
    style planning fill:#BBF7D0,stroke:#16A34A,color:#111827
    style goals fill:#FEF08A,stroke:#CA8A04,color:#111827
    style ledger fill:#FED7AA,stroke:#EA580C,color:#111827
```

ledger and insights never send or hear an event. `shared` (`Money`, `Currency`, error types) is a toolbox every module uses, so it has no arrows.

> [!IMPORTANT]
> Module events run **synchronously, inside the publisher's transaction** ([ADR 007](adr/007-synchronous-module-events.md)). When "Apply this offer" returns, profile, plan and goals are all updated, or none is. The cost: one failing listener rolls everything back, and a slow listener slows the whole request.

**Documenting decisions.** The C4 model draws a system at zoom levels: the *context* (who uses it, what it talks to), then the *containers* (web app, API, database). An ADR (Architecture Decision Record) is one page per decision: Title, Status, Context, Decision, Consequences. It's the written answer to "why did you choose X?". The diagrams are in [ARCHITECTURE.md](ARCHITECTURE.md), the decisions in [adr/](adr/README.md).

> [!TIP]
> Every dependency must earn its place. On Day 3, Spring Modulith's event table and three observability modules were removed: nothing used them, and anything in the project can come up in an interview.

### Check yourself

<details>
<summary>Why does ledger send no event when an expense is logged?</summary>
No other module's own data has to change. insights reads ledger's totals the next time the dashboard opens.
</details>

<details>
<summary>Why doesn't career change the plan directly when an offer is applied?</summary>
That would mean writing into another module's data. career announces OfferApplied, and profile and planning update their own data.
</details>

<details>
<summary>Why does insights read on demand instead of listening to events?</summary>
It stores nothing, so it has nothing to update. Reading when the dashboard opens is always up to date, even after an expense is edited or deleted.
</details>

<details>
<summary>What stops a module from using another module's tables?</summary>
The Spring Modulith test (ModularityTests) checks every dependency between modules and fails the build on a violation.
</details>

## 2. How a request travels

**What an API is.** An API is the list of requests a piece of software accepts from outside, and the only way in. MoneyMacros has it at two scales:

- **Module API:** the Java types in a module's top-level package. identity exposes only `AuthenticatedUser` and `UserRegistered`; everything else is internal.
- **HTTP API:** the URLs the web app calls, such as `GET /api/ping` or `POST /api/auth/login`.

```mermaid
sequenceDiagram
    participant B as Browser, React page
    participant V as Vite dev server, port 5173
    participant S as Spring Boot API, port 8080
    B->>V: GET /api/ping (usePing hook, TanStack Query)
    V->>S: forwards every /api request (vite.config.ts)
    Note over S: security filter chain: /api/ping is public
    S->>S: PingController returns a PingResponse record
    S-->>V: 200 + JSON written by Jackson
    V-->>B: the same response
    B->>B: HomePage shows "API: ok"
```

In development, the browser only talks to Vite on port 5173, which forwards every `/api/...` request to Spring Boot on 8080. The browser sees a single origin, so there is no CORS to configure.

**TypeScript types.** A type describes the shape of data, such as the JSON that `/api/ping` returns. TypeScript checks the code against it while building, then removes it: in the browser, nothing checks the JSON. Since Day 6, the web app's types are generated from the API's contract instead of being written by hand (card below).

**Status codes met so far**

| Code | Meaning | Where |
|---|---|---|
| 200 OK | Success, with a body | `GET /api/me` |
| 201 Created | Something was created | `POST /api/auth/signup` |
| 204 No Content | Success, no body | logout, `GET /api/auth/csrf` |
| 400 Bad Request | Invalid input | validation errors |
| 401 Unauthorized | Not logged in, or wrong credentials | `/api/me` without a session |
| 403 Forbidden | Refused, even when the server knows who you are | a POST without its CSRF token |
| 404 Not Found | No such URL | `/api/does-not-exist` |
| 409 Conflict | Clashes with existing data | an email already in use |
| 500 Internal Server Error | A bug on the server | anything unexpected |

**One error format ([ADR 008](adr/008-problem-details-errors.md)).** Every error is a Problem Detail (RFC 9457) with `type`, `title`, `status`, `detail` and `instance`, sent as `application/problem+json`. A 400 adds an `errors` list of `{field, message}`. A 500 never shows the exception message or a stack trace: it returns an `errorId`, and the same id is written to the server log next to the full error.

### Check yourself

<details>
<summary>Why does the web app call /api/ping and not http://localhost:8080/api/ping?</summary>
Vite forwards /api to the API, so the browser stays on one origin and no CORS setup is needed.
</details>

<details>
<summary>What is the difference between 401 and 403?</summary>
401: the server doesn't know who you are (no session, or wrong credentials). 403: the request is refused anyway, like a POST without its CSRF token.
</details>

<details>
<summary>Why return an errorId instead of the exception message on a 500?</summary>
Messages and stack traces can show internals to an attacker. The errorId lets you find the full error in the log when a user reports it.
</details>

<details>
<summary>Do the generated types check the JSON when the page runs?</summary>
No. TypeScript uses them only while building, then removes them. They're only as true as the contract they come from, which is why the contract must be accurate.
</details>

### Defense card: types from the API contract

```mermaid
flowchart TB
    java["Java records<br/>UserResponse, PingResponse"] -->|springdoc| spec["OpenAPI document<br/>/v3/api-docs"]
    spec -->|"npm run gen:api"| types["schema.d.ts<br/>generated, never edited"]
    types --> client["One API client<br/>openapi-fetch + CSRF header"]
    client --> pages["React pages<br/>typed calls"]

    classDef api fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef contract fill:#A5F3FC,stroke:#0891B2,color:#111827
    classDef web fill:#BFDBFE,stroke:#2563EB,color:#111827
    class java api
    class spec contract
    class types,client,pages web
```

**One sentence:** the API describes itself in an OpenAPI document, and the web app generates its TypeScript types from it, so a change on one side becomes a build error on the other instead of a bug in production.

**For a non-developer:** a restaurant prints its menu once, and every waiter's order pad is printed from that same menu, so nobody can order a dish that doesn't exist.

**In MoneyMacros:** springdoc publishes the contract at `/v3/api-docs`. `npm run gen:api` turns it into `web/src/api/schema.d.ts`, which is committed and never edited by hand. One openapi-fetch client in `web/src/api/client.ts` uses those types and adds the CSRF header to every POST. On Day 6, the always-present fields were marked required and a fake `token` parameter on `/api/auth/csrf` was hidden, so the contract tells the truth ([ADR 005](adr/005-openapi-contract.md)).

**The ladder**

<details>
<summary><b>What?</b> What is OpenAPI?</summary>
A standard, machine-readable description of an API: its URLs, methods, request bodies and responses, written in JSON or YAML.
</details>

<details>
<summary><b>Why?</b> Why generate the types instead of writing them?</summary>
Hand-written types drift away from the server without anyone noticing. Generated ones follow the API, and TypeScript then points at every place in the web app that must change.
</details>

<details>
<summary><b>What if?</b> What if the contract is wrong?</summary>
Then the types are wrong too, so the contract has to be accurate. That's why Day 6 marked the always-present fields as required. The error answers aren't described yet, so the error type is still written by hand, with a comment saying so.
</details>

### Defense card: scoped error handlers

```mermaid
flowchart TB
    auth["AuthController<br/>sign-up, log-in"] --> idh["IdentityExceptionHandler<br/>only for AuthController"]
    idh --> a1["401 or 409<br/>wrong password, email used"]
    other["Other controllers<br/>profile, ledger, from Day 8"] --> gh["GlobalExceptionHandler<br/>everyone else"]
    gh --> a2["400 or 500<br/>validation, unexpected"]
    auth -.->|validation errors| gh

    classDef identity fill:#E9D5FF,stroke:#7C3AED,color:#111827
    classDef shared fill:#E5E7EB,stroke:#6B7280,color:#111827
    class auth,idh,a1 identity
    class other,gh,a2 shared
```

**One sentence:** each error handler answers only for the controllers it's meant for, so log-in's messages never leak into other parts of the app.

**For a non-developer:** each department has its own complaints desk, and the sign-up desk's answer, "wrong password", must never be given at the profile desk.

**In MoneyMacros:** since Day 7, `IdentityExceptionHandler` carries `assignableTypes = AuthController.class`. The dotted arrow: log-in's validation errors (400) still go to `GlobalExceptionHandler`, because the identity handler only knows two kinds of error. "No session" (401) and "no CSRF token" (403) reach neither handler: Spring Security's filters answer before any controller runs.

**The ladder**

<details>
<summary><b>What?</b> What is a controller advice?</summary>
One class that turns exceptions into HTTP answers for many controllers, instead of a try/catch in each one.
</details>

<details>
<summary><b>Why?</b> Why scope it to one controller?</summary>
A message is only true in its context: "Invalid email or password" is right for log-in and wrong anywhere else.
</details>

<details>
<summary><b>What if?</b> Two handlers can handle the same error: which one wins?</summary>
The one with the higher priority, set with @Order. The identity handler goes first for AuthController, and the global one catches everything else.
</details>

## 3. Configuration and dependency injection

**Where a value comes from.** The version returned by `/api/ping` travels from `pom.xml` to the JSON:

```mermaid
flowchart LR
    pom["pom.xml<br/>version 0.0.1-SNAPSHOT"] -->|"build: Maven replaces @project.version@"| yml["target/classes/application.yml<br/>app.version: 0.0.1-SNAPSHOT"]
    yml -->|"startup: Spring resolves ${app.version}"| ctrl["PingController<br/>constructor with @Value"]
    ctrl -->|"GET /api/ping"| json["JSON response<br/>version: 0.0.1-SNAPSHOT"]

    classDef file fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef app fill:#BFDBFE,stroke:#2563EB,color:#111827
    class pom,yml file
    class ctrl,json app
```

Two kinds of placeholder, at two moments:

- `"@project.version@"` is replaced by **Maven, at build time** (resource filtering), in the copy of `application.yml` under `target/classes`.
- `${app.version}` is resolved by **Spring, at startup**. `${DB_PORT:5432}` means "the value of DB_PORT if one is defined, otherwise 5432". When a placeholder has no value and no default, the app refuses to start rather than run with a wrong value.

**YAML.** Indentation is structure. `---` starts a new document; a document with `spring.config.activate.on-profile: dev` applies only to the dev profile. A value that starts with `@` needs quotes.

**`.env`.** Docker Compose reads `.env` automatically; Spring Boot doesn't, so the dev profile imports it with `spring.config.import: "optional:file:../.env[.properties]"`. The file is git-ignored: it's specific to each machine (`DB_PORT=5433` where 5432 is taken) and will later hold secrets. Each machine creates its own from `.env.example`.

**Dependency injection.** An object receives the things it needs instead of creating them. Spring creates the objects (beans) and passes each one its dependencies, usually through the constructor:

```java
public PingController(@Value("${app.version}") String version) {
    this.version = version;   // this.version is the field, version the parameter
}
```

Through the constructor, the dependencies are visible in one place, the field can be `final`, and a test can build the object with `new PingController("1.2.3")` without starting Spring.

### Check yourself

<details>
<summary>What is the difference between @project.version@ and ${app.version}?</summary>
Maven replaces the first while building the project. Spring resolves the second when the app starts.
</details>

<details>
<summary>Why did the API ignore .env on Day 3?</summary>
Spring Boot doesn't read .env files; only Docker Compose does. The dev profile now imports the file explicitly.
</details>

<details>
<summary>Why inject through the constructor rather than into a field?</summary>
The dependencies are visible in one place, the fields can be final, and the class can be created in a plain test without Spring.
</details>

## 4. Money and numbers

**Why never a `double`.** A `double` stores numbers in binary, and most decimal fractions have no exact binary form, the way 1/3 has none in decimal. So `0.1 + 0.2` gives `0.30000000000000004` and `1.0 - 0.9` gives `0.09999999999999998`. Tiny errors add up, and money must be exact.

**The rule ([ADR 002](adr/002-money-minor-units.md)).** Store a `long` count of minor units plus an ISO 4217 currency: 12.30 MAD is `1230` and `MAD`. Calculate with `BigDecimal` built from Strings, and round only with an explicit `RoundingMode`.

| Expression | Result | Lesson |
|---|---|---|
| `new BigDecimal("1.0").subtract(new BigDecimal("0.9"))` | `0.1` | Exact |
| `new BigDecimal(0.1)` | `0.1000000000000000055511151231257827021181583404541015625` | Built from a `double`, it copies the error: use a String |
| `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` | `false` | `equals` also compares the number of decimals |
| `new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))` | `0` | `compareTo` compares the value: use it for amounts |

**Scale.** A `BigDecimal`'s scale is how many digits are *written* after the point. `12.300` has scale 3 but is exactly 12.30 MAD, so `Money.of` accepts it. `12.345` can't fit in 2 decimals without losing value, so it's rejected. `setScale(2, RoundingMode.UNNECESSARY)` means "go to 2 decimals only if nothing is lost, otherwise fail".

In the code, [`Money`](https://github.com/ilyas8888/moneymacros/blob/main/api/src/main/java/com/moneymacros/shared/Money.java) refuses to add two currencies, and `Math.addExact` throws on overflow instead of silently wrapping around. `Money` may be negative, because a balance can go below zero: "an expense must be positive" is a rule of expenses, not of money.

### Check yourself

<details>
<summary>Why is 12.300 MAD accepted but 12.345 MAD rejected?</summary>
12.300 is exactly 12.30, so nothing is lost at 2 decimals. 12.345 would need rounding, and Money refuses to round silently.
</details>

<details>
<summary>1.0 and 1.00 as BigDecimals in a HashSet: how many elements?</summary>
Two. equals and hashCode include the scale, so the set sees two different keys. Compare amounts with compareTo.
</details>

<details>
<summary>Why build a BigDecimal from "0.1" rather than from 0.1?</summary>
The double 0.1 is already slightly wrong, and new BigDecimal(0.1) copies that error exactly.
</details>

<details>
<summary>Why does Money.plus use Math.addExact instead of +?</summary>
A long has a maximum value. Past it, + silently wraps around to a huge negative number, while Math.addExact throws an ArithmeticException, so a wrong amount can't pass unnoticed.
</details>

## 5. Hashing: two families

The same word covers two different tools:

| | HashMap hashing (`hashCode()`) | Password hashing (Argon2id) |
|---|---|---|
| Goal | Find a key fast | Make guessing too expensive |
| Speed | As fast as possible | Slow and memory-hungry on purpose |
| Collisions | Normal, handled inside buckets | Must be practically impossible to find |
| Salt | No | A random one for each password |

**HashMap.** `hashCode()` turns a key into a number, which picks a bucket. Collisions can't be avoided, because there are far more possible keys than hash values. `"Aa"` and `"BB"` both give 2112, since Java computes a String's hash as `h = 31 * h + character`: 65 × 31 + 97 and 66 × 31 + 66. Inside a bucket, `equals()` finds the right key:

```mermaid
flowchart LR
    q["map.get(#quot;BB#quot;)"] -->|"1. hashCode is 2112"| b["bucket 0<br/>2112 mod 16"]
    b -->|"2. walk the bucket"| e1["Aa → 1<br/>equals BB? no"]
    e1 --> e2["BB → 2<br/>equals BB? yes"]
    e2 -->|"3. found"| r["returns 2"]

    classDef step fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef miss fill:#FED7AA,stroke:#EA580C,color:#111827
    classDef hit fill:#BBF7D0,stroke:#16A34A,color:#111827
    class q,b,r step
    class e1 miss
    class e2 hit
```

- A lookup is O(1) on average. The worst case, when everything collides, is O(n), or O(log n) since Java 8, where a crowded bucket becomes a tree.
- At 75% full (the load factor), the table doubles and every key moves to its new bucket.
- The contract: if `a.equals(b)`, then `a.hashCode() == b.hashCode()`. Break it and the map "loses" keys.

**Passwords.** Never store the password, only a one-way hash. A hash is deterministic (same input, same output), so the input is the password plus a **salt**: random bytes drawn at each sign-up. Two users with the same password get different hashes, and precomputed tables of common-password hashes become useless. Fast hashes like SHA-256 or MD5 are the wrong tool, because an attacker can try billions of guesses per second. Argon2id at OWASP's minimum settings (19 MiB of memory, 2 iterations, parallelism 1) makes every guess expensive. In the code, that's `new Argon2PasswordEncoder(16, 32, 1, 19456, 2)` in `SecurityConfig`: a 16-byte salt, a 32-byte hash, then parallelism, memory in KiB and iterations. The stored string carries everything needed to check a password later:

```text
$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>
 │        │    │               │      └ the result
 │        │    │               └ random bytes (the salt)
 │        │    └ settings: memory, iterations, parallelism
 │        └ version
 └ algorithm
```

```mermaid
flowchart TB
    subgraph signup["Sign-up, once"]
        p1["typed password"] --> h1["Argon2id<br/>19 MiB, 2 iterations"]
        s1["new random salt"] --> h1
        h1 --> st["stored in users.password_hash<br/>algorithm, settings, salt, hash"]
    end
    subgraph login["Log-in, every time"]
        p2["typed password"] --> h2["Argon2id<br/>same settings"]
        s2["salt read from the stored string"] --> h2
        h2 --> cmp{"same hash<br/>as stored?"}
        cmp -->|yes| ok["logged in"]
        cmp -->|no| ko["401 Invalid email or password"]
    end
    st -.-> s2

    classDef salt fill:#FEF08A,stroke:#CA8A04,color:#111827
    classDef good fill:#BBF7D0,stroke:#16A34A,color:#111827
    classDef bad fill:#FECDD3,stroke:#E11D48,color:#111827
    class s1,s2 salt
    class ok good
    class ko bad
```

### Check yourself

<details>
<summary>Why do two accounts with the same password have different hashes?</summary>
Each sign-up draws a new random salt, and the hash input is the password plus the salt.
</details>

<details>
<summary>How does log-in check a password that was never stored?</summary>
It reads the salt and settings from the stored string, hashes the typed password with them, and compares the result with the stored hash.
</details>

<details>
<summary>"Aa" and "BB" share a hashCode. How does map.get("BB") still return the right value?</summary>
Both land in the same bucket, and HashMap compares the keys inside it with equals().
</details>

<details>
<summary>Why is slowness a feature for password hashing but a bug for a HashMap?</summary>
An attacker tries billions of password guesses, so each one should be expensive. The app uses a HashMap millions of times itself, so it must be fast.
</details>

## 6. Log-in security: sessions, cookies and CSRF

```mermaid
sequenceDiagram
    participant B as Browser
    participant A as API
    participant D as PostgreSQL
    B->>A: GET /api/auth/csrf
    A-->>B: 204 + cookie XSRF-TOKEN (JavaScript can read it)
    B->>A: POST /api/auth/signup + header X-XSRF-TOKEN
    A->>D: insert the user (Argon2id hash) and a session row
    A-->>B: 201 + cookie SESSION (HttpOnly, SameSite=Lax)
    B->>A: GET /api/me + cookie SESSION
    A->>D: find the session
    A-->>B: 200 with id and email
    B->>A: POST /api/auth/logout + a fresh X-XSRF-TOKEN
    A->>D: delete the session
    A-->>B: 204
    B->>A: GET /api/me
    A-->>B: 401 as Problem Details
```

- **Server-side session ([ADR 003](adr/003-server-side-sessions.md)).** The server remembers who is logged in, in PostgreSQL (table `spring_session`, managed by Spring Session). The browser only holds a random session ID in a cookie. Logging out deletes the session at once, which a JWT stored in the browser can't offer before it expires.
- **HttpOnly.** JavaScript can't read the session cookie, so an XSS bug can't steal it. XSS itself is blocked by React escaping what it displays (and a Content Security Policy on Day 23). SQL injection is blocked by parameterized queries, which Spring Data JPA uses.
- **CSRF.** Another site can make your browser send a request to our API, and the browser attaches our cookies automatically. So every POST must also carry a token only our own page can read: the `XSRF-TOKEN` cookie, copied into the `X-XSRF-TOKEN` header. Another site can't read our cookies, so it can't copy the token (the attack is drawn below). `SameSite=Lax` on the session cookie is a second barrier: browsers leave it off most requests started by other sites.
- **Session fixation.** An attacker plants a session ID before you log in, then reuses it once you're logged in. Changing the ID at log-in makes the planted one useless.
- **User enumeration and timing.** A wrong password and an unknown email get the same `401 Invalid email or password`. Spring Security also hashes a dummy password for unknown emails, so the response time doesn't give them away.
- **Check-then-act race.** "Does this email exist? No, so insert it" lets two simultaneous sign-ups both pass the check. The database's unique constraint is the real guard; since Day 7, its error becomes the 409 (card below).
- **Not OAuth2.** OAuth2 hands log-in to another provider ("Sign in with Google"). MoneyMacros checks the password itself.

**The CSRF attack, step by step.** Our page copies the token from the cookie into a header. The other site's forged request arrives without it:

```mermaid
sequenceDiagram
    participant E as Another site
    participant B as Your browser
    participant A as MoneyMacros API
    Note over B,A: Normal use
    B->>A: GET /api/auth/csrf
    A-->>B: 204 + cookie XSRF-TOKEN=abc
    B->>A: POST + header X-XSRF-TOKEN abc, copied from the cookie
    A-->>B: accepted
    Note over E,A: The attack
    E->>B: a page with a hidden form aimed at MoneyMacros
    B->>A: POST, maybe with your cookies, but no X-XSRF-TOKEN header
    A-->>B: 403 Forbidden
```

> [!NOTE]
> Emails are lower-cased with `Locale.ROOT`. In a Turkish locale, `"I".toLowerCase()` gives a dotless `ı`, so one email could become two different accounts.

### Check yourself

<details>
<summary>Why must the XSRF-TOKEN cookie be readable by JavaScript, while the session cookie must not?</summary>
Our page has to read the token to copy it into a header. The session ID must never be reachable by a script.
</details>

<details>
<summary>Why give the same 401 for a wrong password and an unknown email?</summary>
Different answers would let anyone test which emails have an account.
</details>

<details>
<summary>What is session fixation, and how is it blocked?</summary>
An attacker makes you log in with a session ID they already know. The app changes the session ID at log-in, so the known one stops working.
</details>

<details>
<summary>Why can two sign-ups with the same email both pass the "does it exist?" check?</summary>
Both can ask before either one inserts. Only the database's unique constraint checks and inserts in one step, so it has to be the real guard.
</details>

### Defense card: the sign-up race

```mermaid
sequenceDiagram
    participant A as Request A
    participant B as Request B
    participant D as PostgreSQL
    A->>D: is the email free?
    B->>D: is the email free?
    D-->>A: yes
    D-->>B: yes
    A->>D: insert the user
    D-->>A: saved, so 201 Created
    B->>D: insert the user
    D-->>B: refused by users_email_key
    Note over B: was a 500 before Day 7, now a 409
```

**One sentence:** two sign-ups with the same email at the same instant can both pass the "is it free?" check, so only the database's unique rule can stop the second one.

**For a non-developer:** two cashiers both see the last concert seat as free and both start selling it. Only the central booking system, which records one sale at a time, can refuse the second.

**In MoneyMacros:** `AccountService.register` keeps the quick check for the usual duplicate, then inserts with `saveAndFlush`, so a duplicate fails inside the method instead of at commit time, where it used to become a 500. Only the `users_email_key` violation becomes a 409; any other database error stays a 500, so real bugs aren't hidden. `UserRegistered` goes out only once the row exists.

**The ladder**

<details>
<summary><b>What?</b> What is a race condition?</summary>
Two operations run at the same time, and the result depends on which one is faster.
</details>

<details>
<summary><b>Why?</b> Why keep the check if the database is the guard?</summary>
It answers the usual duplicate quickly. The unique rule covers the rare race.
</details>

<details>
<summary><b>What if?</b> Why not lock the table instead?</summary>
You can't lock a row that doesn't exist yet, and locking the whole table would make every sign-up wait in line. The unique rule does exactly this job, in one atomic step.
</details>

## 7. The web app: state, log-in and languages

Day 6 put the log-in on screen: `/login`, `/signup` and a protected home page, in French and English. Three ideas carry it.

### Defense card: props and state

```mermaid
flowchart LR
    key["You type a letter"] --> input["input<br/>shows its value prop"]
    input -->|"calls its onChange prop"| set["setEmail(text)"]
    set --> state["LoginPage state<br/>email"]
    state -->|"React redraws:<br/>new value prop"| input

    classDef ui fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef memory fill:#BFDBFE,stroke:#2563EB,color:#111827
    class key,input,set ui
    class state memory
```

**One sentence:** props are what a parent gives a child, read-only; state is a component's own memory, and changing it makes React redraw the component.

**For a non-developer:** a chef receives an order from the waiter and can't rewrite it: that's props. His own notepad, where he crosses off what's done, is state: when he updates it, the kitchen works from the new version.

**In MoneyMacros:** `LoginPage` keeps `email` and `password` in `useState`. Each `<input>` receives `value` and `onChange` as props: a controlled input, so the page always knows what's typed. The logged-in user isn't in `useState`: it comes from the server (next card).

**The ladder**

<details>
<summary><b>What?</b> What happens when state changes?</summary>
React runs the component again with the new value and updates only what changed on the page.
</details>

<details>
<summary><b>Why?</b> Why a controlled input?</summary>
The value lives in one place, the component's state, so the page can check it, clear it or send it at any moment.
</details>

<details>
<summary><b>What if?</b> Can a child change its props?</summary>
No. It calls a function the parent passed down, like onChange; the parent changes its state and passes new props. Data flows down, events flow up.
</details>

### Defense card: who is logged in

```mermaid
flowchart TB
    me["useMe: GET /api/me<br/>through TanStack Query"] --> ans{"API answer"}
    ans -->|"200 + user"| yes["Logged in<br/>RequireAuth shows /"]
    ans -->|401| no["Logged out: null<br/>guards send you to /login"]
    later["Log-out, or any<br/>later 401"] --> no

    classDef query fill:#BFDBFE,stroke:#2563EB,color:#111827
    classDef good fill:#BBF7D0,stroke:#16A34A,color:#111827
    classDef bad fill:#FECDD3,stroke:#E11D48,color:#111827
    classDef neutral fill:#E5E7EB,stroke:#6B7280,color:#111827
    class me query
    class yes good
    class no bad
    class ans,later neutral
```

**One sentence:** only the server really knows whether you're logged in, so the app asks it (`GET /api/me`) and every page reads that one answer.

**For a non-developer:** instead of each employee remembering who has a badge, everyone checks the same badge reader at the door.

**In MoneyMacros:** `useMe` keeps the answer in TanStack Query's cache. A 401 there means "logged out" (null), not an error, and it's never retried. `RequireAuth` protects `/`; `RedirectIfAuthed` sends logged-in users away from `/login` and `/signup`. Log-out sets the identity to null itself, and any later 401, such as an expired session, does the same.

**The ladder**

<details>
<summary><b>What?</b> What is server state?</summary>
Data the server owns and the browser only borrows a copy of. It can change without the page knowing, so it must be fetched, cached and refreshed: that's TanStack Query's job.
</details>

<details>
<summary><b>Why?</b> Why not keep the user in useState or localStorage?</summary>
It would go stale, and any script on the page can read localStorage. The session stays in an HttpOnly cookie, and the page simply asks the API.
</details>

<details>
<summary><b>What if?</b> What if the session expires while the page is open?</summary>
The next request answers 401, the app sets the user to null, and the guard sends you to /login.
</details>

### Defense card: French and English

```mermaid
flowchart LR
    en["en: the source of the keys"] --> check{"fr typed as typeof en"}
    check -->|same keys| ok["npm run build passes"]
    check -->|a key missing in fr| ko["The build fails"]

    classDef neutral fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef good fill:#BBF7D0,stroke:#16A34A,color:#111827
    classDef bad fill:#FECDD3,stroke:#E11D48,color:#111827
    class en,check neutral
    class ok good
    class ko bad
```

**One sentence:** every text on screen comes from a translation file, and the build refuses to pass if French is missing a key that English has.

**For a non-developer:** a bilingual menu whose printer refuses to print if a dish has an English name but no French one.

**In MoneyMacros:** i18next and react-i18next. The app uses the browser's language and falls back to French; the FR | EN choice is remembered and sets `<html lang>`, which screen readers use to choose the voice. Error messages are chosen by the web app from the status code, never taken from the API's English text.

**The ladder**

<details>
<summary><b>What?</b> What is i18n?</summary>
Internationalisation (18 letters between the i and the n): preparing an app so its texts, dates and numbers change with the language, without touching the code.
</details>

<details>
<summary><b>Why?</b> Why not show the API's error message?</summary>
It's in English and written for developers. The web app picks a translated message from the status code: 401, 409 or 400.
</details>

<details>
<summary><b>What if?</b> What if a translator forgets a key?</summary>
The build fails before anything ships, because fr must have exactly the keys of en.
</details>

## 8. Testing

| Kind | What runs | Example | Docker? |
|---|---|---|---|
| Unit | One class, no Spring | `MoneyTest` | No, it takes milliseconds |
| Slice | The web layer for one controller | `PingControllerTest` (`@WebMvcTest`) | No |
| Integration | The whole app and a real PostgreSQL 18 | `AuthIntegrationTest` (`@IntegrationTest`) | Yes, through Testcontainers |

- Every test has three parts: **Arrange** the data, **Act** once, **Assert** the result.
- Use your own test values: `PingControllerTest` sets `app.version=1.2.3`, so it checks the controller's job, not the build's.
- Make a new test fail once on purpose. A test you've never seen fail may not test anything.
- Testcontainers starts a real PostgreSQL 18 in Docker, never an in-memory stand-in like H2, so tests run on the same database as production.
- All database tests share one Spring context and one PostgreSQL container, so the suite starts the database once.
- Run `.\mvnw verify` before pushing, so you see a failing test before CI does (section 9).

### Check yourself

<details>
<summary>Why does MoneyTest run without Docker but AuthIntegrationTest doesn't?</summary>
MoneyTest checks one class in memory. AuthIntegrationTest starts the whole app against a real PostgreSQL, which Testcontainers runs in Docker.
</details>

<details>
<summary>Why does PingControllerTest expect 1.2.3 and not the real version?</summary>
The real version changes with every release. A fixed test value proves the controller returns whatever it's given.
</details>

### Defense card: a mock versus the real database

```mermaid
flowchart TB
    subgraph unit["Unit test: milliseconds, no Docker"]
        direction LR
        s1["AccountService"] --> fake["Fake repository<br/>says what we script"]
    end
    subgraph real["Integration test: seconds, real PostgreSQL"]
        direction LR
        s2["AccountService"] --> pg["PostgreSQL in Docker<br/>real unique rule"]
    end
    unit ~~~ real

    classDef code fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef fakeDb fill:#FED7AA,stroke:#EA580C,color:#111827
    classDef realDb fill:#BBF7D0,stroke:#16A34A,color:#111827
    class s1,s2 code
    class fake fakeDb
    class pg realDb
```

**One sentence:** a mock checks that your code reacts correctly to what you assume; only the real thing checks that the assumption is true.

**For a non-developer:** a flight simulator tests the pilot's reactions; only a real flight tests the plane.

**In MoneyMacros:** `AccountServiceTest` fakes the repository, runs in milliseconds, and proves the code turns the constraint error into a 409. `SignupRaceIntegrationTest` starts a real PostgreSQL in Docker with Testcontainers and uses a spy, the real repository with one method overridden, so the "is it free?" check says yes for an email that's taken. The insert then really hits `users_email_key`, and the API answers 409.

**The ladder**

<details>
<summary><b>What?</b> Mock or spy: what's the difference?</summary>
A mock is entirely fake. A spy is the real object with only the parts you override.
</details>

<details>
<summary><b>Why?</b> Why both tests?</summary>
Unit tests are fast and point straight at the bug; integration tests prove the pieces work together for real. Many fast tests and a few real ones: that's the test pyramid.
</details>

<details>
<summary><b>What if?</b> Why does that test start its own Spring context?</summary>
Replacing a bean changes the setup, so Spring can't reuse the shared context. Keeping it in one class limits the cost to a few seconds.
</details>

### Defense card: test the failure path

```mermaid
flowchart TB
    click["Log out while the API is stopped"] --> vite["Vite answers 500<br/>with an empty body"]
    vite --> old{"Day 6 code:<br/>error body?"}
    vite --> now{"Fixed code:<br/>response.ok?"}
    old -->|none, so success| bug["Shows /login<br/>session still alive"]
    now -->|false, so failure| safe["Stays home<br/>says it couldn't log out"]

    classDef neutral fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef bad fill:#FECDD3,stroke:#E11D48,color:#111827
    classDef good fill:#BBF7D0,stroke:#16A34A,color:#111827
    class click,vite neutral
    class old,bug bad
    class now,safe good
```

**One sentence:** decide whether a request worked from its status code, and test what happens when the server fails, not only when it works.

**For a non-developer:** a delivery app that marks a parcel "delivered" because the driver reported no problem, instead of because someone signed for it.

**In MoneyMacros:** with the API stopped, the Vite dev server answers the log-out call with a 500 and an empty body. The first version asked "did an error message come back?". None did, so it showed `/login` while the server session stayed alive. Now every hook decides from `response.ok` (a 2xx status), and log-out only leaves the page once the server confirms it.

**The ladder**

<details>
<summary><b>What?</b> What is a failure-path test?</summary>
A test of what happens when something goes wrong: the server is down, a request times out, the input is bad. Many bugs hide there.
</details>

<details>
<summary><b>Why?</b> Why does it matter for log-out?</summary>
On a shared computer, someone who thinks they logged out may still be logged in for the next person.
</details>

<details>
<summary><b>What if?</b> What if the API answers 401 to the log-out?</summary>
That counts as success: the session is already gone, so the app clears its state and goes to /login.
</details>

## 9. Tools and debugging

**The port clash of Day 5.** The API failed with a French "password authentication failed". The message came from the PostgreSQL installed on Windows, not from Docker's:

```mermaid
flowchart TB
    subgraph before["Before: two servers on port 5432"]
        direction LR
        a1["API, dev profile<br/>DB_PORT is 5432"] -->|"localhost:5432"| n1["PostgreSQL 15 installed on Windows<br/>no user moneymacros: password error"]
        a1 -.->|"never reached"| d1["Docker PostgreSQL 18<br/>also published on 5432"]
    end
    subgraph after["After: .env sets DB_PORT=5433"]
        direction LR
        a2["API, dev profile<br/>reads .env, DB_PORT is 5433"] -->|"localhost:5433"| d2["Docker PostgreSQL 18<br/>user moneymacros: connected"]
    end
    before ~~~ after

    classDef idle fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef bad fill:#FECDD3,stroke:#E11D48,color:#111827
    classDef good fill:#BBF7D0,stroke:#16A34A,color:#111827
    class a1,a2,d1 idle
    class n1 bad
    class d2 good
```

Habits from these five days:

- **Logs:** `BUILD SUCCESS` only means Maven finished. Search for `Application run failed`, then read the last `Caused by`.
- **Compile errors:** read the first `[ERROR]`. It gives the file, the `[line,column]` and the reason. "cannot find symbol" means Java doesn't know a name: a typo (`string` for `String`) or a missing import.
- **Flyway:** every schema change is a new migration (`V1__create_users.sql`, then `V2__spring_session.sql`), never an edit to one already applied. Flyway lists what already ran in its `flyway_schema_history` table and runs only the new files. It refuses to start when an applied migration is missing, so deleting one means resetting the local database (`docker compose down -v`). Hibernate only checks that the tables match the code (`ddl-auto: validate`) and never changes them.
- **Docker:** `docker compose up -d` starts the database. `docker compose ps` shows the port mapping, such as `0.0.0.0:5433->5432/tcp`: port 5433 on your PC leads to port 5432 inside the container, and `DB_PORT` in `.env` sets the first number. The data lives in the named volume `moneymacros_pgdata`, so recreating the container keeps it, and `docker compose down -v` deletes it. postgres:18 keeps its data in `/var/lib/postgresql/18/docker`, so the volume is mounted at `/var/lib/postgresql`.
- **Git:** Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`, `ci:`), the GitHub noreply email, no logs or "try" commits in the public repo, and a compile before every push. Anything pushed stays in the history, so a pushed secret must be changed: deleting the file doesn't remove it. From Day 6, one branch and one pull request per day.
- **CI:** on every push and pull request, GitHub Actions runs two jobs: `api` (`./mvnw -B verify`, every test) and `web` (`npm ci`, lint, build). Green means the project builds and passes its tests on a clean machine, not only on yours.
- **Windows:** `.\mvnw`, quotes around `-D` arguments in PowerShell, `mvnw` marked executable for the Linux CI, and CRLF line endings kept for `.cmd` and `.bat` files.
- **Claude Code:** start it from the repo root so `CLAUDE.md` loads. Rules in `.claude/rules/` load only for the files they cover. Read its plan before saying OK.

### Check yourself

<details>
<summary>The log ends with BUILD SUCCESS, but the app isn't running. How can both be true?</summary>
Maven finished its own job, which was launching the app. The app then failed during startup: the real error is higher up, after "Application run failed".
</details>

<details>
<summary>Why did a password error mean "wrong database" on Day 5?</summary>
Two PostgreSQL servers listened on port 5432, and localhost reached the one installed on Windows, which has no moneymacros user. DB_PORT=5433 in .env sends the API to Docker's.
</details>

<details>
<summary>Why never edit a migration that has already run?</summary>
Flyway records each applied migration with a checksum. A changed or missing one fails validation, and other databases would end up with a different schema.
</details>

### Defense card: the pull request workflow

```mermaid
flowchart LR
    branch["Day branch"] --> pr["Pull request"]
    pr --> ci["CI: api and web jobs"]
    ci --> review{"Review"}
    review -->|fix commits| pr
    review -->|approved| merge["Merge into main"]

    classDef neutral fill:#E5E7EB,stroke:#6B7280,color:#111827
    classDef good fill:#BBF7D0,stroke:#16A34A,color:#111827
    class branch,pr,ci,review neutral
    class merge good
```

**One sentence:** each day's work lives on its own branch and reaches main only through a pull request, once CI passes and the code is reviewed.

**For a non-developer:** an article is proofread before it's printed in the newspaper, not after.

**In MoneyMacros:** since Day 6, one branch per day (`day-06-auth-pages`, `day-07-catch-up`), a pull request on GitHub, two CI jobs (api and web), a review, and only then the merge. Fix commits pushed to the branch update the pull request by themselves. After the merge: delete the branch, then `git switch main; git pull`.

**The ladder**

<details>
<summary><b>What?</b> What is a pull request?</summary>
A request to merge one branch into another, with the changes, the CI results and the review discussion in one place.
</details>

<details>
<summary><b>Why?</b> Why not push straight to main?</summary>
main must always build and run. A branch lets you try, review and fix without breaking it.
</details>

<details>
<summary><b>What if?</b> What if CI fails on the pull request?</summary>
Nothing reaches main. You push a fix commit to the same branch, CI runs again, and you merge once it's green.
</details>

## 10. Algorithms

- **Big-O** describes how time or memory grows with the input size n. With 5 times more data, an O(n) loop takes about 5 times longer, and an O(n²) loop about 25 times.
- **Two Sum, brute force:** try every pair with two nested loops, `j` starting at `i + 1` so an element is never paired with itself. O(n²) time, O(1) extra space. Loop over positions (`nums[i]` is the value), compare `nums[i] + nums[j]` with the target, and return positions, not values.
- **Still open, the O(n) version:** the partner of `nums[i]` is always `target - nums[i]`, and it can be any number already walked past. Next step: fill the table (number, partner needed, in memory?, memory after) for `[2, 7, 11, 15]` with target 9, remembering each number with its position.

> [!TIP]
> To try an algorithm without a project, Java 25 runs a single file with no class around it ([JEP 512](https://openjdk.org/jeps/512)). Write `void main() { IO.println("Hi"); }` in `TwoSum.java` and run `java TwoSum.java`. Classes like `HashMap` and `List` need no import.

### Check yourself

<details>
<summary>Why does j start at i + 1 in the brute force?</summary>
So an element is never added to itself, and each pair is checked only once.
</details>

<details>
<summary>What is the brute force's time complexity, and why?</summary>
O(n²): for each of the n numbers, it scans the rest of the array.
</details>

## 11. Interview bank so far

Write your own version of each answer in `docs/private/interview-bank.md`, in 3 to 5 sentences.

| Day | Question | A good answer covers |
|---|---|---|
| 1 | Present your project in 60 seconds. | What it does and for whom; what's different (it links the budget to job offers); the stack in one line |
| 2 | Monolith or microservices for this project, and why? | One developer, one deployable; what microservices would cost; boundaries enforced by a test; a module can be extracted later |
| 3 | What is dependency injection, and why through the constructor? | Objects receive their dependencies and Spring wires them; visible dependencies, `final` fields, easy tests; `PingController` and `@Value` |
| 4 | Why is money never stored as a `double`? | Binary can't hold most decimal fractions (`0.1 + 0.2`); errors add up; minor units in a `long` and `BigDecimal` built from Strings; `Money` and ADR 002 |
| 5 | How do you store passwords safely? | Never the password; a random salt for each one; a slow, memory-hard algorithm (Argon2id); log-in re-hashes with the stored salt; the same error for a wrong password and an unknown email |
| 6 | What are props and state in React? | Props: read-only inputs from the parent; state: the component's memory, and changing it redraws it; a controlled input (`value` + `onChange`); `LoginPage`; the user comes from TanStack Query, not `useState` |
| 7 | Why sessions and not JWT? | A server session in an HttpOnly cookie: scripts can't read it, and log-out revokes it at once; the trade-off is CSRF, covered by the token; a future mobile app would get its own token-based login |
| Any | How did you use AI on this project? | Claude Code wrote code from prompts you prepared; you made the design decisions (the ADRs), reviewed every change, ran the checks, fixed what failed, and can explain each part |

## 12. Problems you solved

"Tell me about a problem you solved" is a classic interview question. Answer it with **STAR**: the Situation, your Task, the Action you took, and the Result. All but the first story also answer "How did you use AI?": you checked what the tool produced and caught what it missed.

**The wrong database (Day 5)**

- **Situation and task:** the API had to start for the log-in check, but PostgreSQL rejected the password, in French.
- **Action:** read the first real error and saw it came from the PostgreSQL installed on Windows, not Docker's. `docker compose ps` and `.env` showed why: both servers used port 5432, and `.env` was missing. Set `DB_PORT=5433` in `.env` (section 9).
- **Result:** the API reached Docker's database and the manual check passed. Lesson: check *which* server answers, not only what it says.

**A table nothing used (Day 3)**

- **Situation and task:** the generated backend created an `event_publication` table, and every part of the project had to be explainable.
- **Action:** found that it stores events for delivery later, in the background, which [ADR 007](adr/007-synchronous-module-events.md) rules out. Removed the dependency and the migration that created it.
- **Result:** a smaller schema in which every table has a reason. Lesson: don't ship what you can't explain.

**Links to files that didn't exist (Day 3)**

- **Situation and task:** five files cited `.claude/rules/backend.md` and `.claude/rules/frontend.md` in their comments, but neither file existed.
- **Action:** checked the folder, then had both files written from the ADRs.
- **Result:** every reference now leads somewhere. Lesson: verify what a tool says it did.

**Log-out that pretended to work (Day 6)**

- **Situation and task:** with the API stopped on purpose, clicking Log out still went to `/login`, as if it had worked.
- **Action:** traced it: the Vite dev server answered 500 with an empty body, and the code decided success from the error body instead of the status. Had every hook decide from `response.ok`, and log-out leave the page only once the server confirms it.
- **Result:** log-out is honest: if the server can't end the session, the page says so. Lesson: test the failure path, and trust the status code.

**The sign-up race (Day 7)**

- **Situation and task:** a review found that two sign-ups with the same email at the same instant could end in a 500.
- **Action:** made the insert happen inside the method, turned only the email's unique-constraint error into a 409, and proved it twice: a fast unit test and a test against the real PostgreSQL.
- **Result:** a clean 409 every time. Lesson: the database is the real guard, and a mock only proves your own assumptions.
