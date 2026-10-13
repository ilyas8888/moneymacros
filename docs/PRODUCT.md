# MoneyMacros — Product

## Vision
MoneyMacros plans money the way a nutrition app plans food. Young graduates
choosing their first job rarely know what a salary *actually* means for daily
life. MoneyMacros turns net monthly income into a "calorie budget," a budget
method into a "diet," and Needs / Wants / Future (savings) into "macros." Every
expense is a logged meal; the dashboard shows what is safe to spend today, how
balanced the plan is (Budget-Score A–E), and whether goals are on track.

A career simulator converts job offers into real take-home pay, so a graduate
can compare offers side by side and see each one's effect on the budget and on
goal dates. The mental model *is* the product: nutrition is intuitive and
budgeting is not, so we borrow the vocabulary people already understand.

## Personas
**Salma — new engineering graduate (Morocco).**
Has two job offers and no idea which leaves her better off after IR tax and
CNSS. Wants to compare take-home pay, see how each offer changes her savings
rate and how soon she could move out, then choose with confidence. Mobile-first,
bilingual (FR/AR), values clarity over jargon.

**Youssef — first job, ~1 year in (Morocco).**
Earns a stable salary and wants structure. Building a 3-month emergency fund and
saving for a used car. Needs safe-to-spend today, a healthy Budget-Score, and an
honest answer on whether his car goal date is realistic at his current spending.

## MVP scope — due 10 Nov 2026
- **Accounts:** email sign-up, login, password reset.
- **Onboarding:** net income, budget method (50/30/20, 70/20/10, custom),
  currency (MAD).
- **Expense diary:** log expenses against Needs / Wants / Future and categories;
  recurring-expense templates ("recipes").
- **Dashboard:** safe-to-spend today, macro rings (Needs / Wants / Future),
  Budget-Score A–E, month overview.
- **Goals:** named goal with target amount and target date; progress and a
  projected completion date.
- **Career comparator (Morocco 2026):** enter 1–2 offers → net take-home, effect
  on budget and goal dates, 5-year projection, apply one offer to the plan.
- **Demo mode:** one click loads Salma's data; fully explorable with no signup.
- **Deployed** to a public URL with CI (build + tests on every push).

## Out of scope for the MVP (planned later)
- France tax rules and MAD/EUR side-by-side comparison.
- Reverse career mode (the salary needed to hit a goal by a date).
- CSV / bank import; AI quick-log of expenses.
- 2FA, Arabic RTL layout, Expo mobile app.
- Shared / household budgets, multi-currency accounts, live bank sync.

## Guardrails
- **Education, not advice.** Planning and literacy only — never investment, tax,
  or legal advice. Tax logic is a transparent estimate, clearly labelled.
- **Show the math.** Every computed number (net pay, safe-to-spend, score) can
  reveal the formula and the inputs behind it. No black boxes.
- **Privacy first.** Minimal personal data; no bank credentials; demo mode needs
  no signup; data is the user's to export or delete.
- **Honest numbers.** Estimates are labelled as estimates, with their
  assumptions (year, region, rule set) visible.

## Success criteria for 10 Nov 2026
1. A visitor can open the deployed URL and explore via demo mode with no signup.
2. A new user can sign up, onboard, and log an expense in under three minutes.
3. The dashboard shows safe-to-spend, macro rings, and a Budget-Score from the
   user's real data.
4. A user can create a goal and see a projected completion date.
5. The Morocco comparator turns two offers into net pay and shows each one's
   effect on the budget and on goal dates, with a 5-year projection.
6. Every headline number can reveal the formula behind it.
7. The app is deployed to a public URL; CI runs build + tests on every push and
   is green.
