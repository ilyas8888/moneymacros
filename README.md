# MoneyMacros

**Status:** 🚧 In progress — MVP targeted for **10 Nov 2026**.

## Pitch

<!-- TODO: write the pitch here. -->
Cette application de gestion des finances, développée en framework spring boot et React, sert a aider les jeunes diplomés, chercheurs du travail ou travailleur qui veullent organiser leurs finances en conrrespondant a leurs salaires, comparer le post desiré avec leurs besions et projets financiers, et trouver le meilleur emplois pour y corespendre.
On suit une architecture Monolithe modulaire, une seule base de données PostgreSQL, un seul projet Spring Boot, composé de plusieurs modules.
l'application serait sécurisée avec OAuth2, avec des mot de passe hashé avec Argon2id, les cookies stocké dans HttpOnly comme bonne pratique pour éviter les attaques XSS.
2 points principale qui distinguent notre application:
-la personalisation marocaine: cette application suit precisement la loi marocaine en se que correspend au TAX et interets et loi a propos du salaire.
-la liaison entre gestion des budgets et calcul de salaires, avec un assistant ia pour automatiser: on trouve beacoups des application dans le marché qui offre gestions de budget, mais trés peu qui lie le budget avec la situation professsionelle personalisée comme notre.
## Docs

- [Product](docs/PRODUCT.md) — vision, personas, MVP scope, guardrails, success criteria.
- [Glossary](docs/GLOSSARY.md) — the nutrition → money vocabulary.
- [Working rules](CLAUDE.md) — how this repo is built, and the task REPORT format.

## Tech (planned)

- `api/` — Spring Boot backend (Java 25, Temurin)
- `web/` — React frontend
- `docs/` — product & reference docs
