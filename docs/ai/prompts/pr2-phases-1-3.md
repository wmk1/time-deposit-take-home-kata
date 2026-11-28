# Implementation prompt - PR 2 (Phases 1-3)

Author agent: Claude Code. Reviewer: Codex (shared prompt `docs/ai/prompts/review.md`).
Start Claude Code in the repository root and send:
"Read docs/ai/prompts/pr2-phases-1-3.md and carry it out."

---

You implement PR 2 of `docs/PLAN.md`: Phases 1-3 (steps 5-11). Read `AGENTS.md`, `README.md`
and `docs/PLAN.md` first. All rules in `AGENTS.md` apply; the two rules that decide this
assignment are **test quality** and **no dead code**.

You are explicitly asked to commit each step below yourself (Conventional Commits, no AI trailers).
Do not push. Do not touch `kotlin/`, `python/`, `c#/`, `typescript/`.

## Step 0 - Branch

- Create branch `feat/calculator-refactoring` from `main`.
- Commit this prompt file: `docs: add PR 2 implementation prompt`

## Step A - Characterization tests (use the `test-writer` subagent)

Goal: pin the CURRENT behaviour of `TimeDepositCalculator.updateBalance` before any refactoring.
No production code changes in this step.

- Replace the placeholder test (`assertThat(1).isEqualTo(1)`) - delete it, do not keep it.
- Parameterized tests (`@ParameterizedTest` + `@CsvSource`) with readable display names.
- Expected balances are literals derived by hand: `balance + round_half_up(balance * rate / 12)`,
  with the arithmetic in a comment. Never call production code to compute an expected value.
- Cover at least:
  - day boundaries: 30 / 31 (all plans), 45 / 46 (premium), 365 / 366 (student);
  - each plan type with a normal case;
  - unknown plan type and different letter case (`"Basic"`) -> no interest;
  - zero balance;
  - rounding quirk of `new BigDecimal(double)`: basic, balance `18.00`, day 31. The exact double
    interest is `0.01499999...`, so the result is `18.01` (`BigDecimal.valueOf` would give `18.02`).
    Also basic `6.00` -> `6.01` (double interest is slightly above 0.005);
  - several deposits in one call: each updated independently, the list updated in place,
    `id`, `planType` and `days` unchanged.
- Assert exact `double` values (`isEqualTo`), not `isCloseTo`: behaviour must be bit-for-bit identical.
- Commit: `test: pin current updateBalance behaviour with characterization tests`

After this commit the characterization test file must not change again in this PR, except for being
moved by `git mv` in Step B (content identical). If you believe an expected value is wrong, stop and
report it instead of editing it.

## Step B - Multi-module build (Phase 2)

- `java/pom.xml` becomes the parent (`packaging pom`, Spring Boot parent kept, `java.version` 21).
- Create `java/time-deposit-domain` and move the existing sources with `git mv`
  (package `org.ikigaidigital` unchanged). The domain POM has test dependencies only - no Spring.
- Do NOT create empty adapter or app modules: an empty module is dead code. Each module is created
  in PR 3 together with its first real content. Update step 8 of `docs/PLAN.md` accordingly.
- `./mvnw test` from `java/` must still run all tests.
- Commit: `build: convert java/ into a multi-module build with a domain module`

## Step C - Enforce the Java version

- Add `maven-enforcer-plugin` to the parent with `requireJavaVersion` `[21,)` and a clear message.
- Commit: `build: fail fast with a clear message on Java older than 21`

## Step D - Refactor the calculator (Phase 3)

Target design (adjust if you have a better one, but justify it in a code comment):
- `InterestPolicy` - open interface (NOT sealed: new plans must be addable), e.g.
  `boolean supports(String planType)` and `double monthlyInterest(TimeDeposit deposit)`
  returning the raw, unrounded interest (0 when not eligible).
- `BasicInterestPolicy`, `StudentInterestPolicy`, `PremiumInterestPolicy`.
- `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` keeps its signature, finds the policy for
  each deposit (none found -> no interest), and keeps the rounding exactly as today:
  `new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).doubleValue()` added to the balance as `double`.
- Keep the double arithmetic order `balance * rate / 12` exactly - a different order can change results.
- The no-arg constructor keeps working with the three default policies. A constructor taking a list
  of policies is allowed only together with a test that uses it for real (e.g. a custom plan type).
- Where the "no interest in the first 30 days" rule lives is a design decision: avoid duplicating it
  in three classes, and explain the choice in a comment.
- Small commits (e.g. introduce the interface and the first policy, then the next ones, then remove
  the old branching). All tests green after every commit; no leftover branching code.
- Unit tests per policy are welcome, but must not duplicate the characterization tests one-to-one.

## Step E - Self-check before you finish

1. Run the `architecture-reviewer` subagent and fix what it reports.
2. `./mvnw verify` in `java/` is green; `git diff --check main...HEAD` is clean.
3. `git diff main...HEAD -- <characterization test file>` shows only the Step A content and the move.
4. No unused classes, methods, constructors, imports or dependencies; no TODOs or commented-out code.
5. Report: list of commits, design decisions and their reasons, anything you were unsure about.
