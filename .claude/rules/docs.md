---
paths: ["docs/**"]
---

# Docs rules

Rules for everything under `docs/`: diagrams, public docs, and the Obsidian-only
learning notes. See also the global Obsidian rule in `CLAUDE.md`.

## a. Module and C4 colors

Every Mermaid diagram and every canvas uses the same palette. **Text is always
`#111827`** for module colors; C4 nodes use **white** text.

**Modules** (fill / stroke):

| Module | Fill | Stroke |
|--------|---------|---------|
| identity | `#E9D5FF` | `#7C3AED` |
| profile | `#BFDBFE` | `#2563EB` |
| planning | `#BBF7D0` | `#16A34A` |
| ledger | `#FED7AA` | `#EA580C` |
| goals | `#FEF08A` | `#CA8A04` |
| career | `#FECDD3` | `#E11D48` |
| insights | `#A5F3FC` | `#0891B2` |
| shared | `#E5E7EB` | `#6B7280` |

In a Mermaid `flowchart`, apply with one `style` line per node, e.g.
`style identity fill:#E9D5FF,stroke:#7C3AED,color:#111827`.

In a `sequenceDiagram`, wrap each module participant in a colored `box` (Mermaid
cannot fill a single participant otherwise). The box uses the module's **stroke
color at 18% opacity**, so the box tint is faint and the participant text stays
readable in both light and dark themes:

| Module | Box color |
|--------|-----------|
| identity | `rgba(124,58,237,0.18)` |
| profile | `rgba(37,99,235,0.18)` |
| planning | `rgba(22,163,74,0.18)` |
| ledger | `rgba(234,88,12,0.18)` |
| goals | `rgba(202,138,4,0.18)` |
| career | `rgba(225,29,72,0.18)` |
| insights | `rgba(8,145,178,0.18)` |
| shared | `rgba(107,114,128,0.18)` |

Example: `box rgba(225,29,72,0.18) career`.

**C4 views** use the classic C4 colors, white text:

| Role | Fill |
|------|------|
| person | `#08427B` |
| system | `#1168BD` |
| container | `#438DD5` |
| external / after MVP | `#999999` (dashed) |

## b. Public docs (`docs/*.md`, `docs/adr/`)

These render on GitHub, so use **only GitHub-compatible callouts**:
`> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`. No Obsidian-only
callout types, and no `[[wikilinks]]` — relative Markdown links only.

## c. Learn notes (`docs/learn/`)

Obsidian-only, git-ignored. **One note per concept, under 90 lines.** Obsidian
needs the YAML front matter on line 1, so the order is:

1. Front matter: `tags: [learn]`, `day: N`.
2. A link back to `Start here`.
3. `> [!abstract] In one sentence` — the whole idea in a sentence.
4. `## The picture` — exactly one colored Mermaid diagram (palette above).
5. `## How it works` — numbered steps.
6. `## In MoneyMacros` — relative links to `../ARCHITECTURE.md`, the ADRs or
   code.
7. `> [!example] Analogy` — the nutrition metaphor when it fits.
8. `> [!warning] Common mistake`.
9. `## Check yourself` — three folded callouts `> [!question]- <question>`, each
   holding its answer.
10. `## In my own words` — left empty, with the comment
    `<!-- written by me, not by Claude -->`.

Learn notes may use the richer Obsidian callouts (`[!abstract]`, `[!question]`,
`[!example]`) because they never reach GitHub.
