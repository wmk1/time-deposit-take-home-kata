# Implementation prompt - PR 3 (Phases 4-7)

Author agent: Codex. Reviewer: Claude Code (shared prompt `docs/ai/prompts/review.md`).
Start Codex in the repository root and send:
"Read docs/ai/prompts/pr3-phases-4-7.md and carry it out."

---

You implement PR 3 of `docs/PLAN.md`: Phases 4-7 (domain ports, persistence, REST, application).
Read `AGENTS.md`, `README.md` and `docs/PLAN.md` first. All rules in `AGENTS.md` apply; the two rules
that decide this assignment are **test quality** and **no dead code**. The Claude Code Stop hook does
not run for you: run the tests yourself after every step.

You are explicitly asked to commit each step below yourself (Conventional Commits, no AI trailers).
Do not push. Do not touch `kotlin/`, `python/`, `c#/`, `typescript/`. Docker is available.

## Decisions already made by the developer (do not change them)

- `POST /time-deposits/balance-updates` returns **204 No Content** (command-query separation);
  results are read with `GET /time-deposits`.
- The balance update of all deposits runs in **one transaction** (all or nothing).
- Demo data lives in a **separate Flyway location** (e.g. `classpath:db/demo`) that is enabled only
  when the application is run locally, never in tests.
- Persistence uses `JdbcClient` and plain SQL (no JPA). REST is contract-first (OpenAPI + generator).
- `TimeDeposit` and `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` stay unchanged.
- Modules are created together with their first real content - no empty modules.

## Step 0 - Branch

- Create branch `feat/api-and-persistence` from `main`.
- Commit this prompt file: `docs: add PR 3 implementation prompt`

## Step 1 - Domain: read model, ports, services (module `time-deposit-domain`, pure Java)

- `Withdrawal` (record: id, amount, date) and a read model combining a `TimeDeposit` with its
  withdrawals (record). `TimeDeposit` itself is not modified.
- Inbound ports: one use case to update all balances, one to get all deposits with withdrawals.
- Outbound port(s): only the methods the services actually call (no speculative CRUD).
- Services implement the inbound ports and delegate interest to `TimeDepositCalculator`.
- Unit tests with in-memory fakes of the outbound ports (no mocking framework needed).
- Withdrawals do not affect interest - state this assumption in a comment.
- Commits: `feat(domain): add withdrawal read model and use case ports`,
  `feat(domain): implement use case services`

## Step 2 - Persistence adapter (new module `time-deposit-adapter-persistence`)

- Flyway `V1`: tables exactly as in README - `timeDeposits` (id, planType, days, balance) and
  `withdrawals` (id, timeDepositId, amount, date). Quoted camelCase identifiers so the names match,
  `NUMERIC(19,2)` for money, foreign key from withdrawals to time deposits.
- Adapter on `JdbcClient` implementing the outbound port(s); SQL in text blocks.
- Writing the balance: `BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP)` - the domain
  keeps `double`, the database keeps 2 decimals. Explain this in a comment.
- The update writes only `balance`; `days` is not changed (assumption, comment it).
- Integration tests `*IT` on Testcontainers PostgreSQL with the real migrations (Failsafe).
  Cover: reading deposits with and without withdrawals, saving balances, 2-decimal storage.
- Commits: `feat(persistence): add database schema migrations`,
  `feat(persistence): add JDBC adapter for time deposits and withdrawals`

## Step 3 - REST adapter (new module `time-deposit-adapter-rest`)

- `openapi.yaml` with exactly two operations: `GET /time-deposits` (200, array of
  `{id, planType, balance, days, withdrawals: [{id, amount, date}]}`) and
  `POST /time-deposits/balance-updates` (204).
- `openapi-generator-maven-plugin` (generator `spring`, interface only). Check that the generator
  version and options produce code that compiles with Spring Boot 4 / Jakarta (e.g. disable
  `openApiNullable` if it pulls in unneeded dependencies). Never edit generated code.
- Controller implements the generated interface, maps domain -> DTO, calls the inbound ports only.
- Swagger UI via springdoc-openapi 3.x showing the contract.
- Controller tests (MockMvc or MockMvcTester) with the inbound ports replaced by simple fakes or mocks
  of the ports (never mock the controller itself). Assert status codes and exact JSON.
- Commits: `feat(rest): add OpenAPI contract and generated API`,
  `feat(rest): implement time deposit controller`

## Step 4 - Application (new module `time-deposit-app`)

- Spring Boot main class and a configuration that wires domain services and adapters explicitly.
- The transaction boundary for "update all balances" lives outside the domain (e.g. a decorator in
  the app module using `TransactionTemplate`), so the domain stays framework-free.
- Local run: `compose.yaml` with PostgreSQL (Spring Boot Docker Compose support is fine), demo data
  from the separate Flyway location covering every plan type and the day boundaries, plus withdrawals.
- End-to-end test `*IT` (Testcontainers): seed data, `POST` -> 204, `GET` -> exact balances and
  withdrawals; and a check that the published API (`/v3/api-docs`) has exactly two operations.
- No actuator, no extra endpoints, no virtual threads.
- Commits: `feat(app): add Spring Boot application with wiring and transaction boundary`,
  `feat(app): add local run setup with PostgreSQL and demo data`,
  `test(app): add end-to-end test of both endpoints`

## Step 5 - Self-check before you finish

1. Check the change against `.claude/agents/architecture-reviewer.md` (you have no subagent for it -
   go through its checklist yourself) and fix what you find.
2. `./mvnw verify` in `java/` is green (Docker running); `git diff --check main...HEAD` is clean.
3. The application starts locally and both endpoints work in Swagger UI - describe how you checked.
4. No unused classes, methods, imports or dependencies; no TODOs; no commented-out code;
   no generated code committed by hand.
5. Report: list of commits, design decisions and their reasons, assumptions, anything you were unsure about.
