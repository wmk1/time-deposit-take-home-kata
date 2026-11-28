# Time Deposit Kata - Java solution

Submission for the XA Bank time deposit refactoring kata. The original assignment is kept at the end of
this file.

- **Implementation:** Java 21, Spring Boot 4.1, PostgreSQL - everything lives in `java/`.
- The `kotlin/`, `python/`, `c#/` and `typescript/` folders are the reference implementations from the
  assignment. One implementation meets the requirements, so they were deliberately left untouched.
- **A note from the author:** [docs/AUTHOR-NOTE.md](docs/AUTHOR-NOTE.md) - how I approached the task, in my own words.
- **AI-assisted development:** see [docs/AI.md](docs/AI.md) (tools, setup, what was AI-assisted) and
  [docs/ai/review-log.md](docs/ai/review-log.md) (every AI review finding with the developer's verdict).

## Prerequisites

- **JDK 21 or newer.** The recruiter's instructions ask for Java >= 17; this solution targets Java 21 and the
  build stops with a clear message on an older JDK.
- **Docker** with Compose - for the local PostgreSQL and for the Testcontainers integration tests.
- No Maven installation is needed: the Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.16.

## Run the application

```sh
cd java
docker compose up -d --wait
./mvnw package
java -jar time-deposit-app/target/time-deposit-app-1.0-SNAPSHOT.jar --spring.profiles.active=local
```

On Windows PowerShell, set the profile with an environment variable - Windows PowerShell can split the
`--spring.profiles.active=local` argument at its dots before Java sees it:

```powershell
cd java
docker compose up -d --wait
.\mvnw.cmd package
$env:SPRING_PROFILES_ACTIVE = "local"
java -jar time-deposit-app/target/time-deposit-app-1.0-SNAPSHOT.jar
# After stopping the application, clear the profile before running the tests:
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

The `local` profile connects to the PostgreSQL from
`java/compose.yaml` and loads demo data. Without that profile the application runs only the schema migrations
and reads the database settings from `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD`.

To start again from the original demo data: stop the application, run `docker compose down -v` and then
`docker compose up -d --wait` (two commands, so they also work in Windows PowerShell 5.1).

## Trigger the endpoints with Swagger

The contract file is `java/time-deposit-adapter-rest/src/main/openapi/openapi.yaml`. `/v3/api-docs` is generated at
runtime from the controller that implements the interface generated from that file.

1. Open <http://localhost:8080/swagger-ui.html>.
2. **GET /time-deposits** - *Try it out*, *Execute*. You get the 9 demo deposits (all with balance 1200.00)
   and their withdrawals.
3. **POST /time-deposits/balance-updates** - *Try it out*, *Execute*. Expected response: **204 No Content**.
4. **GET /time-deposits** again. Expected balances after one update:

| id | planType | days | balance after one POST | why |
|---|---|---|---|---|
| 1 | basic | 30 | 1200.00 | no interest in the first 30 days |
| 2 | basic | 31 | 1201.00 | 1200 * 0.01 / 12 = 1.00 |
| 3 | student | 30 | 1200.00 | no interest in the first 30 days |
| 4 | student | 31 | 1203.00 | 1200 * 0.03 / 12 = 3.00 |
| 5 | student | 365 | 1203.00 | last day of the first year |
| 6 | student | 366 | 1200.00 | no interest after one year |
| 7 | premium | 45 | 1200.00 | premium interest starts after 45 days |
| 8 | premium | 46 | 1205.00 | 1200 * 0.05 / 12 = 5.00 |
| 9 | unknown | 46 | 1200.00 | unknown plan type: no interest |

JSON shows balances as numbers, so 1201.00 appears as `1201` or `1201.0`, not `1201.00`.

Every POST applies one more month of interest. The same calls with curl:

```sh
curl http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/balance-updates
```

## Tests

```sh
cd java
./mvnw test     # unit tests, no Docker needed
./mvnw verify   # unit + integration tests on Testcontainers PostgreSQL (Docker must be running)
```

- **Characterization tests** (`TimeDepositCalculatorTest`) pin `updateBalance` bit-for-bit before and after
  the refactoring: day boundaries (30/31, 45/46, 365/366), every plan, unknown plans and letter case, zero and
  negative balances, the rounding of `new BigDecimal(double)` and the order of the double operations.
  Expected values are computed by hand in comments, never by calling production code.
- Policy and extensibility tests, use case tests with in-memory fakes, JDBC adapter integration tests
  (including a test that concurrent updates wait for the row lock), controller tests, and an end-to-end test
  of both endpoints that also checks the published API has exactly two operations.

## Architecture

Hexagonal architecture as a multi-module Maven build; the dependencies point to the domain only.

```
java/
+-- time-deposit-domain/              pure Java, no framework
|     TimeDeposit, TimeDepositCalculator          (unchanged API)
|     InterestPolicy + Basic/Student/Premium      (strategy per plan type)
|     Withdrawal, TimeDepositWithWithdrawals      (read model)
|     port.in / port.out, service                 (use cases)
+-- time-deposit-adapter-persistence/ Flyway migrations, JdbcClient repository
+-- time-deposit-adapter-rest/        openapi.yaml -> generated API interface, controller
+-- time-deposit-app/                 Spring Boot main, wiring, transaction boundary, local setup
```

## Design decisions

- **Interest rules as strategies.** Each plan is an `InterestPolicy`; a new plan is a new class, no existing
  code changes (Open/Closed). The interface is deliberately not sealed. The "no interest in the first 30 days"
  rule lives in a shared base class of the existing plans, so a future plan can pay from day one.
- **Behaviour preserved exactly.** `TimeDeposit` and the `updateBalance(List<TimeDeposit>)` signature are
  unchanged. The calculator still rounds with `new BigDecimal(double)` and HALF_UP and keeps the operation order
  `balance * rate / 12`, both pinned by tests.
- **Contract-first REST.** The OpenAPI file is the source of truth; the controller implements the generated
  interface. Swagger UI is served by springdoc.
- **Plain SQL.** `JdbcClient` and Flyway; two tables do not justify JPA, and the domain object maps
  easily by hand. Table and column names are exactly those of the assignment.
- **Transactions.** The update of all balances runs in one transaction (all or nothing). The boundary is in the
  application module (`TransactionTemplate`), so the domain stays framework-free. The read for the update locks
  the rows (`FOR NO KEY UPDATE`), so two concurrent updates are applied one after the other instead of losing
  a month of interest.
- **POST returns 204 No Content** - it is a command (command-query separation); the result is read with GET.

## Assumptions

The assignment leaves these points open; each is also explained in a code comment where it applies.

- `days` is stored state; the balance update does not change it.
- The balance update is not idempotent: every call applies one month of interest. It is meant to be triggered
  once a month (e.g. by a scheduler).
- Withdrawals are historical records returned by GET; they do not affect interest.
- The domain keeps `double` balances (the `TimeDeposit` API stays unchanged); the database stores
  `NUMERIC(15,2)`, written with `BigDecimal.valueOf(balance).setScale(2, HALF_UP)`. Balances go up to
  9,999,999,999,999.99: up to 15 significant digits a `double` keeps every cent exactly.
- Plan type matching is case-sensitive (`"Basic"` gets no interest), as in the original code.
- A `null` plan type gets no interest. The original code threw a `NullPointerException` after day 30 and
  aborted the whole batch; `null` is invalid data and is excluded by the `NOT NULL` column, and reproducing an
  accidental exception would carry a defect of the old code into the new design.
- Negative balances get negative interest, exactly as in the original code.
- Demo data lives in a separate Flyway location (`db/demo`) that only the `local` profile loads; tests and any
  other environment start with an empty schema. It is an `afterMigrate` callback, not a versioned migration: it
  inserts the demo rows only when they do not exist yet.
- Swagger UI and `/v3/api-docs` are documentation of the two business operations, not additional endpoints;
  no actuator is included.

---

## Original assignment

The text of the assignment as provided (headings moved one level down).

### XA Bank Time Deposit

#### Context
A junior developer implemented domain logic for a time deposit system but did not complete the API functionality. Your task is to refactor the existing codebase to implement all required functionalities based on the provided business requirements, ensuring no breaking changes occur.

#### Requirements

1. **API Endpoints**:
    - Create a RESTful API endpoint to update the balances of all time deposits in the database.
    - Create a RESTful API endpoint to retrieve all time deposits.
        - The GET endpoint should return a list of all time deposits with the following schema:
            - `id`
            - `planType`
            - `balance`
            - `days`
            - `withdrawals`

2. **Database Setup**:
    - Store all time deposit plans in a database.
    - Define the following tables:
        - `timeDeposits`:
            - `id`: Integer (primary key)
            - `planType`: String (required)
            - `days`: Integer (required)
            - `balance`: Decimal (required)
        - `withdrawals`:
            - `id`: Integer (primary key)
            - `timeDepositId`: Integer (foreign key, required)
            - `amount`: Decimal (required)
            - `date`: Date (required)

3. **Interest Calculation**:
    - Implement logic to calculate monthly interest based on the plan type:
        - **Basic Plan**: 1% interest
        - **Student Plan**: 3% interest (no interest after 1 year)
        - **Premium Plan**: 5% interest (interest starts after 45 days)
    - No interest is applied for the first 30 days for any existing plans.

4. **Refactoring Constraints**:
    - Do not introduce breaking changes to the shared `TimeDeposit` class or modify the `updateBalance` method signature.
    - Ensure the design is extensible to accommodate future complexities in interest calculations.

5. **Code Quality**:
    - Adhere to SOLID principles, design patterns, and clean code practices where applicable.

6. **AI-Assisted Development**:
    - Set up an AI harness or agent workflow and use it throughout the development for this take-home exercise.
    - Briefly document the tools and setup used (e.g., LLMs, coding assistants, agentic frameworks, configuration).
    - Ensure your AI setup is practical and reproducible.
    - Include any custom rules, system prompts, or agent configurations used.
    - Include a brief summary of which parts of the solution were AI-assisted and why.

#### Important Guidelines
- The existing `TimeDepositCalculator.updateBalance` method is functioning correctly. Ensure its behavior remains unchanged after refactoring.
- The final solution must include **exactly two API endpoints**. Do not develop additional endpoints.
- **Do not** create a pull request or a new branch in the ikigai-digital repository. Instead, fork the repository into your own GitHub repository and develop the solution there.
- Handling invalid input or exceptions is not required.
- Use any tools, frameworks, or libraries you find suitable.
- In case of ambiguity, make logical assumptions and justify them in code comments.

#### Preferred Stack
- Use an OpenAPI Swagger contract.
- Embrace Hexagonal Architecture.
- Follow atomic commit practices.
- Utilize testcontainers.
- Leverage AI-assisted development tools for code generation, testing, and refactoring.

#### Submission Instructions
- Provide clear instructions on how to trigger the endpoints using the Swagger contract.
- Email the link to your public GitHub repository.
