---
paths: ["web/**"]
---

# Frontend rules

- Server state goes through TanStack Query only, with the single `QueryClient` in `main.tsx`. No `fetch` inside `useEffect`.
- Code is grouped by feature: `src/features/<feature>/` holds its pages, components and hooks.
- No `any`; ESLint enforces it.
- API types are generated from the OpenAPI contract and never edited by hand. A hand-written type is a temporary exception and says so in a comment. (ADR 005)
- Call the API with relative `/api/...` URLs. Vite proxies them in dev, so there is no CORS to configure.
- No money math in the browser: the API computes amounts, and the web app only formats minor units plus currency with `Intl.NumberFormat`. (ADR 002)
- User-facing text goes through i18n once it is set up (see `CLAUDE.md`).
- `npm run lint` and `npm run build` must pass before a commit (CI runs both). Prettier handles formatting.
