# Implementation Plan — XA Bank Time Deposit Kata

Implementation language: **Java** (`java/` folder). The other language folders
(`kotlin/`, `python/`, `c#/`, `typescript/`) are intentionally left untouched as reference
implementations — the requirements are met by a single implementation.

## Evaluation context

- **Deadline: 28 November, 18:00.** Scope below is cut to what fits; nothing optional is started
  before the required parts are done and verified.
- The most common rejection reasons named by the recruiter: **poorly written tests** and
  **AI mistakes such as dead code**. Both are hard rules in `AGENTS.md` and explicit categories
  in the review prompt.
- An accepted solution is followed by a 1-hour live coding session that extends it with new
  requirements: the design must be easy to change, and every line must be understood by the developer.
- The brief asks for Java >= 17; this solution targets Java 21 (prerequisite stated in README).

### PR plan

| PR | Phases | Author | Reviewer |
|---|---|---|---|
| 1 | 0 — foundations, AI harness | Claude (chat) | Codex + Claude Code (calibration) |
| 2 | 1–3 — characterization tests, modules, calculator refactoring | Claude Code | Codex |
| 3 | 4–7 — domain, persistence, REST, application | Codex | Claude Code |
| 4 | 8 — documentation, fixes from the final review | Claude Code | Codex + Claude Code (final, blind) |

Only if time remains after PR 4: CI on GitHub Actions (JDK 21 and 25).

## Stack decisions

| Area | Choice | Why |
|---|---|---|
| Language | Java 21 | LTS; records, pattern matching for `switch`, sequenced collections |
| Framework | Spring Boot 4.1 | Spring Boot 3.5 reached end of OSS support in June 2026 |
| API contract | OpenAPI 3 (`openapi.yaml`), contract-first via `openapi-generator-maven-plugin` | Preferred stack; contract is the source of truth |
| API docs / UI | springdoc-openapi 3.x (Swagger UI) | Matches Spring Boot 4 |
| Persistence | PostgreSQL, Flyway, `JdbcClient` | Two tables, immutable domain object — plain SQL is clearer than JPA |
| Tests | JUnit 6 (Jupiter, managed by Spring Boot), AssertJ, Testcontainers, MockMvc | Preferred stack; behaviour preservation proof |
| Architecture | Hexagonal, multi-module Maven | Boundaries enforced by the compiler, visible in the project tree |

## Module layout

```
java/
├─ pom.xml                            parent (Java 21, Spring Boot BOM)
├─ time-deposit-domain/               pure Java, no Spring
│    org.ikigaidigital.TimeDeposit, TimeDepositCalculator   (same package = same FQNs)
│    interest policies, Withdrawal, read model
│    port.in:  UpdateAllBalancesUseCase, GetAllTimeDepositsUseCase
│    port.out: TimeDepositRepository (+ withdrawals read port)
│    service:  use case implementations
├─ time-deposit-adapter-rest/         openapi.yaml → generated API interfaces, controller, mappers
├─ time-deposit-adapter-persistence/  Flyway migrations, JdbcClient adapter, Testcontainers tests
└─ time-deposit-app/                  Spring Boot main, wiring, E2E tests
```

Ports and use cases live in the domain module (separate packages). A separate `application`
module is not justified for two use cases.

## Hard constraints (from README)

- Do not introduce breaking changes to `TimeDeposit`; do not change the `updateBalance` signature.
- `TimeDepositCalculator.updateBalance` behaviour must stay **bit-for-bit identical**
  (boundaries `> 30`, `< 366`, `> 45`; HALF_UP rounding of interest via `new BigDecimal(double)`;
  unknown plan types get no interest).
- Exactly **two** API endpoints: `GET /time-deposits`, `POST /time-deposits/balance-updates`.
- No PRs/branches in the ikigai-digital repository — all work happens in the fork.

## Working agreement

- **Atomic commits** — one logical change per commit, tests green after every commit.
- **Branch + PR per group of phases** (see PR plan) inside the fork; PRs are the unit of AI review.
- Commit messages: Conventional Commits (`feat:`, `refactor:`, `test:`, `docs:`, `build:`, `chore:`).

## AI cross-review workflow

Two agents: **Claude Code** and **Codex**. Shared rules live in `AGENTS.md`
(Codex reads it natively; `CLAUDE.md` imports it), so both work from identical context.

1. **Author alternates per PR** (see PR plan): the agent that did not write the code reviews it.
2. **One shared review prompt** — `docs/ai/prompts/review.md` — used verbatim by both tools,
   always against the same input (`git diff main...<branch>` + `AGENTS.md`).
   The prompt requires a fixed output format (one finding per row: severity, category,
   file:line, problem, suggested fix) so findings can be pasted straight into the log.
3. **Human triage.** Every finding is judged by the developer:
   `valid` / `partially valid` / `false positive` / `duplicate` / `won't fix` (with reason).
4. **Log everything** in `docs/ai/review-log.md`: PR, author, reviewer, finding, verdict,
   action taken (commit SHA or reason for rejection).
5. **Calibration:** PR 1 and the final review are done by both agents independently (blind),
   to compare them on identical input.
6. **Final comparison** in `docs/AI.md`: findings per tool, precision (valid / total),
   severity mix, unique catches, false positives, qualitative notes, plus an honest summary
   of which parts of the solution were AI-assisted and why.

Implementation prompts are task-specific: each one references a step from this plan plus `AGENTS.md`.
Notable prompts are stored in `docs/ai/prompts/`.

## Phases

### Phase 0 — Foundations and AI harness
1. `.gitattributes` (`* text=auto`) — normalise line endings.
2. Build upgrade: Java 21, Spring Boot 4.1 parent (dependency/plugin management only), Maven Wrapper.
   The multi-module parent structure comes in Phase 2.
3. Harness: `AGENTS.md`, `CLAUDE.md` (imports `AGENTS.md`), `.claude/agents/`
   (`test-writer`, `architecture-reviewer`), `/review-pr` skill, a Claude Code Stop hook running
   unit tests when `java/` changed, `docs/ai/prompts/review.md`, `docs/ai/review-log.md`.
4. `docs/AI.md` skeleton, filled in as the work progresses.

### Phase 1 — Safety net (before any refactoring)
5. Characterization tests for `updateBalance` (parameterized table): day boundaries 30/31, 45/46,
   365/366; unknown plan type and letter case; HALF_UP rounding edge cases; large balances;
   multiple deposits in one call.
6. Remove the placeholder test (`assertThat(1).isEqualTo(1)`) — it is replaced, not kept.

### Phase 2 — Multi-module restructure
7. `git mv` existing code into `time-deposit-domain` (package `org.ikigaidigital` unchanged,
   file history preserved).
8. Empty `adapter-rest`, `adapter-persistence`, `app` modules wired into the parent POM.

### Phase 3 — Calculator refactoring
9. `InterestPolicy` — open interface: `supports(planType)`, `monthlyInterest(deposit)`.
10. `BasicInterestPolicy`, `StudentInterestPolicy`, `PremiumInterestPolicy` — one commit each,
    characterization tests green throughout.
11. `TimeDepositCalculator` delegates to policies; the no-arg constructor keeps the default set,
    an additional constructor accepts custom policies (Open/Closed). Signature and output unchanged.

### Phase 4 — Domain and ports
12. `Withdrawal` (record) and read model `TimeDepositWithWithdrawals` (record) — `TimeDeposit`
    stays untouched.
13. Inbound ports `UpdateAllBalancesUseCase`, `GetAllTimeDepositsUseCase`; outbound port(s) for
    deposits and withdrawals.
14. Use case services + unit tests with an in-memory fake repository.

### Phase 5 — Persistence adapter
15. Flyway `V1`: `timeDeposits` and `withdrawals` tables exactly as specified (quoted identifiers,
    `balance NUMERIC(19,2)`).
16. Flyway `V2`: demo data covering every plan type and day boundary, plus withdrawals.
17. `JdbcClient` adapter with SQL text blocks.
18. Integration tests on Testcontainers (PostgreSQL).

### Phase 6 — REST adapter (contract-first)
19. `openapi.yaml`: `GET /time-deposits`, `POST /time-deposits/balance-updates`; response schema
    `id`, `planType`, `balance`, `days`, `withdrawals`.
20. Generated API interfaces + DTOs; controller implements them; domain → DTO mapper.
21. MockMvc controller tests.

### Phase 7 — Application
22. `app` module: Spring Boot main, wiring configuration (policies as beans),
    `docker-compose.yml` with PostgreSQL for local runs.
23. E2E test on Testcontainers: seed → POST → GET → assert balances.
24. Test asserting the contract exposes exactly two operations.

### Phase 8 — Documentation and final review
25. README: how to run, how to trigger endpoints via Swagger UI, assumptions, why Java only.
26. `docs/AI.md` completed with the Claude Code vs Codex comparison from the review log.
27. Final independent review of the whole solution by both agents, then fixes.

## Java 21 features — where they fit

- **Records**: `Withdrawal`, read model, DB row mapping. `TimeDeposit` stays a class —
  turning it into a record would rename accessors and break its API.
- **Pattern matching for `switch`**: plan-type and error mapping.
- **Sealed interfaces**: deliberately *not* used for `InterestPolicy` — it would contradict the
  extensibility requirement (documented in code).
- **Sequenced collections** (`getFirst()`) in tests.

## Assumptions to document (in code comments and README)

- `days` is stored state and is **not** incremented by the balance update (the calculator never changes it).
- The balance update is **not idempotent** — each call applies one month of interest
  (intended to be triggered by a monthly scheduler).
- Withdrawals do not affect interest calculation; they are returned by the GET endpoint only.
- Balance is persisted as `NUMERIC(19,2)`; the domain keeps `Double` to avoid breaking `TimeDeposit`.
- Swagger UI / `/v3/api-docs` are documentation, not business API endpoints; no actuator is added.
