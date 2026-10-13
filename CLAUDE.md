# CLAUDE.md

MoneyMacros plans money the way a nutrition app plans food, helping young
graduates turn job offers into real take-home pay and keep a healthy budget.
This repo is a monorepo: **`api/`** is the Spring Boot (Java 25, Temurin)
backend, **`web/`** is the React frontend, and **`docs/`** holds product and
reference docs. Start with `docs/PRODUCT.md` for scope and `docs/GLOSSARY.md`
for the nutrition → money vocabulary used across code and UI.

## Layout
```
api/    Spring Boot backend (Java 25, Temurin)   # created later
web/    React frontend                           # created later
docs/   PRODUCT.md, GLOSSARY.md                  # product + reference
        ARCHITECTURE.md, adr/                    # design + ADRs, created later
```

## Commands
<!-- TODO: fill on Day 3, once api/ and web/ exist. -->
- Build: TODO
- Test: TODO
- Run (api): TODO
- Run (web): TODO
- Lint / format: TODO

## Working rules
- **One task per session.** Do the task asked — nothing extra.
- **Plan first.** For any change touching more than one file, show a short plan
  and wait for my OK before editing.
- **Stay in scope.** Never refactor code outside the current task.
- **Justify dependencies.** One line on *why*, before adding any new dependency.
- **Never commit secrets.** No credentials, tokens, or `.env` files in git.
- **PowerShell only.** Every command you give me is Windows PowerShell syntax
  (no bash, no WSL).
- **Conventional Commits.** e.g. `feat:`, `fix:`, `docs:`, `chore:`, `ci:`.
- **English, with i18n.** Code, comments, commit messages and docs in English;
  user-facing text only through i18n.
- **End every task with the REPORT** (format below).

## REPORT format
End each task with this block, 20 lines max:
```
Done: <what changed, 1–2 lines>
Files changed: <paths>
How to verify: <exact PowerShell commands to run>
Decisions made: <choices + 1-line reason each>
Questions for me: <blockers or confirmations, or "none">
```
