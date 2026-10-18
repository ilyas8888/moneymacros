# MoneyMacros
[![CI](https://github.com/ilyas8888/moneymacros/actions/workflows/ci.yml/badge.svg)](https://github.com/ilyas8888/moneymacros/actions/workflows/ci.yml)
**Status:** 🚧 In progress — MVP targeted for **10 Nov 2026**.

## Pitch

### Application Overview

Developed using Spring Boot and React, this financial management application helps recent graduates, job seekers, and working professionals organize their finances around their salaries. It allows users to align their career goals with their financial needs and personal projects, and to compare job offers by their real impact on budget and goals.

### Security

To ensure robust security, user passwords are hashed using Argon2id. The session ID lives in an HttpOnly cookie, so JavaScript cannot read it and an XSS bug cannot steal the session.

### Key Differentiator

Integration of Budgeting and Salary Calculations: While many financial apps offer general budget management, very few connect personal budget planning directly with a user's customized career and salary situation.

## Architecture

The application follows a Modular Monolith architecture:

Backend: A single Spring Boot project structured into distinct modules.

Database: A single PostgreSQL database.

See [Architecture](docs/ARCHITECTURE.md) for the C4 views, module map, data
model and flow, and the [Architecture Decision Records](docs/adr/README.md) for
the decisions behind them.

## Docs

- [Product](docs/PRODUCT.md) — vision, personas, MVP scope, guardrails, success criteria.
- [Glossary](docs/GLOSSARY.md) — the nutrition → money vocabulary.
- [Working rules](CLAUDE.md) — how this repo is built, and the task REPORT format.

## Tech (planned)

- `api/` — Spring Boot backend (Java 25, Temurin)
- `web/` — React frontend
- `docs/` — product & reference docs
