# AGENTS.md — shared instructions for all coding agents

This file is the single source of truth for every AI agent working in this repository
(Codex reads it natively, Claude Code imports it from `CLAUDE.md`). Keep it short and factual.

## Project

XA Bank time deposit refactoring kata (take-home assignment). Requirements: `README.md`.
Implementation plan and current phase: `docs/PLAN.md` — always read it before starting a task.

- Implementation language: **Java 21**, Maven, Spring Boot 4.1 — everything lives in `java/`.
- `kotlin/`, `python/`, `c#/`, `typescript/` are reference implementations. **Never modify them.**

## Hard constraints (from README — never violate)

1. `org.ikigaidigital.TimeDeposit`: no breaking changes (constructor, getters, `setBalance` stay as they are).
2. `TimeDepositCalculator.updateBalance(List<TimeDeposit>)`: signature unchanged and behaviour
   **bit-for-bit identical** — boundaries `days > 30`, student `days < 366`, premium `days > 45`;
   interest rounded HALF_UP via `new BigDecimal(double)` and added as `double`; unknown plan types
   get no interest. Characterization tests pin this; never change an expected value to make a test pass.
   Accepted exception: a `null` plan type gets no interest (the original code threw a NullPointerException;
   see README assumptions).
3. Exactly **two** API operations: `GET /time-deposits`, `POST /time-deposits/balance-updates`.
4. Ambiguities are resolved by explicit assumptions written in code comments (and listed in README).

## Architecture (hexagonal)

- `time-deposit-domain`: pure Java. No Spring, Jakarta, JDBC or Jackson imports. Contains
  `TimeDeposit`, `TimeDepositCalculator`, interest policies, ports (`port.in`, `port.out`) and use case services.
- Adapters (`adapter-rest`, `adapter-persistence`) depend on the domain, never on each other.
- `time-deposit-app` only wires things together (Spring Boot main, configuration).
- REST is contract-first: edit `openapi.yaml`, never edit generated sources by hand.

## Commands (run from `java/`)

- Unit tests (fast, no Docker): `./mvnw test`
- Full build incl. integration tests (Testcontainers, needs Docker): `./mvnw verify`
- Windows PowerShell: `.\mvnw.cmd test`

## Testing conventions

Weak tests are the most common reason this kind of submission is rejected. Every test must earn its place.

- JUnit 6 (Jupiter API; version managed by Spring Boot) + AssertJ. Unit tests: `*Test` (Surefire, no Spring context, no Docker).
  Integration tests: `*IT` (Failsafe, Testcontainers).
- Each test verifies one behaviour; its name says which (`@DisplayName` or a descriptive method name).
- Assert exact expected values, computed independently of the code under test (show the arithmetic in a comment).
  Never compute the expected value by calling the production code.
- Forbidden: tests without assertions, `assertThat(1).isEqualTo(1)`-style placeholders, asserting only
  "not null" when a value can be checked, mocking the class under test, tests that only repeat another test.
- Cover boundaries explicitly (e.g. day 30/31, 45/46, 365/366) rather than "typical" values only.
- Every behaviour change starts with a failing test; every refactoring keeps all tests green.

## Code style

- Java 21 idioms where they fit: records for immutable data, pattern matching for `switch`,
  `var` only when the type is obvious. `TimeDeposit` must stay a class (records would break its API).
- Small classes, constructor injection, no field injection, no Lombok.
- Comments explain *why* (assumptions, trade-offs), not *what*.

## No dead code (hard rule)

AI-generated leftovers are the second most common rejection reason. Before finishing any task:

- No unused classes, methods, constructors, parameters, fields, imports or dependencies.
- No commented-out code, no `TODO`/`FIXME`, no placeholder or example code, no `System.out` debugging.
- No speculative abstractions "for the future": every interface, class and method must be used by
  production code or by a test that verifies real behaviour. Extensibility comes from the design
  (e.g. interest policies), not from unused hooks.
- No duplicate implementations of the same logic.
- Delete code you replaced; do not keep "old" versions next to new ones.

## Git workflow

- One branch + PR per PR group from the "PR plan" in `docs/PLAN.md`, inside the fork `wmk1/time-deposit-take-home-kata`.
  **Never** open PRs against `ikigai-digital/*`.
- Atomic commits, Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `build:`, `chore:`).
  Tests must pass after every commit.
- Commit only when the developer asks. Do not add `Co-Authored-By` or other AI attribution
  trailers — AI involvement is documented in `docs/AI.md` and `docs/ai/review-log.md`.
- Never push, force-push or rewrite history unless explicitly asked.

## Reviews

When asked to review a branch or PR, follow `docs/ai/prompts/review.md` exactly — the same prompt
is used by every agent so that their reviews can be compared. Do not add criteria or change the output format.
