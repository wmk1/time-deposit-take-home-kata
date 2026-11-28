# AI cross-review log

Every finding from an AI review is recorded here and judged by the developer.
The process is described in `docs/PLAN.md` (section "AI cross-review workflow");
the review prompt is `docs/ai/prompts/review.md`. The comparison of tools lives in `docs/AI.md`.

## Legend

- **Author / Reviewer**: `Claude Code`, `Codex`, `Claude (chat)` (+ model name and settings).
- **Severity** (as reported by the reviewer): `blocker`, `major`, `minor`, `nit`.
- **Verdict** (developer's judgement): `valid`, `partial`, `false positive`, `duplicate`, `won't fix`.
- **Action**: commit SHA of the fix, or the reason why nothing was changed.
  "log commit" = the commit that recorded the entry in this file.
- **ID**: `PR<pr>-<X|C><run>-<n>` (X = Codex, C = Claude; the surface is named in the Reviewer line).

Each PR section lists: plan phase, author, reviewers per run, review mode, date, and one table per run
with the columns ID, Severity, Category, Location, Finding, Verdict, Action.

## Summary

| PR | Run | Author | Reviewer | Findings | Valid | Partial | Duplicate | False positive | Won't fix |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | Claude (chat) | Codex, GPT-6.2-Sol, default | 3 | 3 | 0 | 0 | 0 | 0 |
| 1 | 2 | Claude (chat) | Codex, GPT-6.2-Sol, default | 3 | 3 | 0 | 0 | 0 | 0 |
| 1 | 2 | Claude (chat) | Claude (chat, new session), Claude Opus 5.5 | 11 | 7 | 2 | 0 | 0 | 2 |
| 1 | 3 | Claude (chat) | Codex, GPT-6.2-Sol, xhigh | 7 | 6 | 1 | 0 | 0 | 0 |
| 2 | 1 | Claude Code | Codex, GPT-6.2-Sol, xhigh | 3 | 2 | 0 | 0 | 0 | 1 |
| 3 | 1 | Codex | Claude (chat, new session), Claude Opus 5.5 | 10 | 7 | 0 | 0 | 0 | 3 |

---

## PR 1 - Phase 0: foundations and AI harness

- Plan phase / steps: Phase 0, steps 1-4
- Author: Claude (chat)
- Review mode: calibration (blind, separate sessions)
- Date: 2026-11-27

### Run 1 - Codex (GPT-6.2-Sol, default settings)

Reviewed an incomplete branch: the harness commits had not been made yet (finding PR1-X1-2).

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR1-X1-1 | major | correctness | `java/mvnw:1` | Wrapper committed as `100644` from Windows; `./mvnw` fails with "permission denied" on Unix | valid | `0ea8929` |
| PR1-X1-2 | major | documentation | `docs/PLAN.md:83` | Harness and AI docs described in the plan are untracked, the PR would not contain them | valid | `4d56f27`, `473ea8d`, `46401d8`, `7bcc9dd`, `3ac96aa` |
| PR1-X1-3 | minor | documentation | `java/pom.xml:30` | Spring Boot 4.1 manages JUnit 6.0.3, docs say JUnit 5 | valid | Docs switched to JUnit 6 instead of pinning JUnit 5 (would fight the Spring Boot BOM): `4d56f27`, `473ea8d`, `3ac96aa` |

### Run 2 - Codex (GPT-6.2-Sol, default settings), complete branch

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR1-X2-1 | major | correctness | `java/mvnw.cmd:92` | `(Get-Item $MAVEN_M2_PATH).Target[0]` indexed without a null check; wrapper fails before Maven starts | valid | Upstream bug in maven-wrapper 3.3.4, fix (apache/maven-wrapper@7443d4e) unreleased; patched locally with a null guard: `2b4312e` |
| PR1-X2-2 | major | correctness | `.claude/hooks/run-unit-tests.sh:17` | Stop hook checks only the working tree; Java changes committed during the turn skip the tests | valid | Hook now compares `java/` with `main` as well: `c4ce9c2` |
| PR1-X2-3 | minor | documentation | `docs/AI.md:24` | Docs promise tests must pass, but the hook lets Claude stop after one retry | valid | Superseded by PR1-X3-3 (behaviour fixed, not only docs): `c4ce9c2`, `docs/AI.md` in log commit |

### Run 2 - Claude (chat, new session; Claude Opus 5.5, self-reported), complete branch

Could not run the tests (its VM had JDK 11 only); read the Surefire report of the developer's last run instead.

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR1-C2-1 | minor | correctness | `docs/AI.md:37` | Windows PowerShell 5.1 reads the UTF-8 prompt as ANSI; em dashes reach Codex as `â€”`, so the agents get different input | valid | Prompt made ASCII-only: `6c912dc`; `-Encoding UTF8` in `docs/AI.md` (log commit) |
| PR1-C2-2 | minor | commits | `java/mvnw:1` | Exec-bit fix is a separate commit, so `./mvnw` fails on Unix in earlier commits; squash suggested | won't fix | Rewriting history would erase the review -> fix trail this log points to; the branch was already pushed |
| PR1-C2-3 | minor | documentation | `.claude/hooks/run-unit-tests.sh:10` | Docs promise more than the hook does after one retry | valid | Same as PR1-X2-3: `c4ce9c2` |
| PR1-C2-4 | minor | documentation | `docs/ai/review-log.md:52` | Action column holds SHA placeholders | valid | Filled in (log commit) |
| PR1-C2-5 | minor | documentation | `docs/PLAN.md:16` | "The brief asks for Java >= 17", but the README names no version | partial | The requirement comes from the recruiter's e-mail, which the reviewer had not seen; wording now names the source and that the README prerequisite comes in Phase 8: `268a2ed` |
| PR1-C2-6 | nit | dead code / AI artifacts | `docs/AI.md:9` | `_fill in_` placeholders although values are known | partial | Known values filled in; sections that are explicitly work in progress stay (log commit) |
| PR1-C2-7 | nit | documentation | `docs/PLAN.md:86` | Verdict names differ between plan and log; no Duplicate column | valid | `268a2ed`, log commit |
| PR1-C2-8 | nit | documentation | `docs/PLAN.md:82` | Plan describes the review input differently from the prompt | valid | `268a2ed` |
| PR1-C2-9 | nit | dead code / AI artifacts | `docs/ai/review-log.md:23` | Empty template block committed next to real entries | valid | Replaced by a format description in the Legend (log commit) |
| PR1-C2-10 | nit | design | `.claude/agents/architecture-reviewer.md:4` | "Read-only" subagent has Bash, so the rule is only an instruction | won't fix | Bash is needed for `git diff`; documented as a known limitation in `docs/AI.md` |
| PR1-C2-11 | nit | dead code / AI artifacts | `.gitattributes:11` | `*.jar binary` rule for a file that does not exist (only-script wrapper) | valid | `48a0446` |

### Run 3 - Codex (GPT-6.2-Sol, xhigh reasoning), same commit and same prompt as run 2

Experiment: only the reasoning effort changed. The model labelled itself "GPT-6" in the report header.

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR1-X3-1 | major | correctness | `java/mvnw.cmd:92` | Same as PR1-X2-1, with the cause pinned to Windows PowerShell 5.1 returning a null Target | valid | `2b4312e` |
| PR1-X3-2 | major | correctness | `.claude/hooks/run-unit-tests.sh:17` | Same as PR1-X2-2 | valid | `c4ce9c2` |
| PR1-X3-3 | major | correctness | `.claude/hooks/run-unit-tests.sh:10` | After one blocked stop the hook exits without re-running tests, so Claude can finish with failures | valid | Follow-up stop now re-runs the tests and reports a non-blocking warning to the developer if they still fail: `c4ce9c2` |
| PR1-X3-4 | minor | documentation | `docs/AI.md:37` | PowerShell example mangles the UTF-8 prompt (`â€”`) | partial | Problem valid (same as PR1-C2-1); the suggested `codex exec - < file` does not work in PowerShell (`<` is reserved): `6c912dc` |
| PR1-X3-5 | minor | documentation | `docs/PLAN.md:16` | Plan says the Java 21 prerequisite is in the README, but it is not | valid | `268a2ed` |
| PR1-X3-6 | minor | documentation | `AGENTS.md:74` | `AGENTS.md` says one PR per phase, the plan groups phases | valid | `268a2ed` |
| PR1-X3-7 | nit | style | `docs/ai/review-log.md:27` | Trailing spaces in the template fail `git diff --check` | valid | Template removed (log commit) |

### Notes on PR 1

- Codex at xhigh kept every finding from the default run, found 4 more, and upgraded the hook issue
  from "document it" to "fix the behaviour".
- Codex received the mangled prompt in runs 2 and 3 (clipboard via PowerShell 5.1). It did not notice
  at default effort, but did at xhigh. Claude read the prompt file directly, so it had clean input.
- Only Codex found functional defects (Windows wrapper, hook logic) - it ran the wrapper and the hook logic.
  Claude found more process and documentation inconsistencies and the encoding issue.
- Claude could not run the tests in its environment; Codex did.

## PR 2 - Phases 1-3: characterization tests, multi-module build, calculator refactoring

- Plan phase / steps: Phases 1-3, steps 5-11 (prompt: `docs/ai/prompts/pr2-phases-1-3.md`)
- Author: Claude Code
- Review mode: regular (one reviewer)
- Date: 2026-11-28

Before the review the developer, with Claude (chat), spot-checked the riskiest part: the characterization
test file had not changed since its commit (only moved by `git mv`), and the arithmetic in its comments was
recomputed independently. The null plan type change was noticed there, but not passed to the reviewer, so
the review stayed blind.

### Run 1 - Codex (GPT-6.2-Sol, xhigh reasoning)

The model again labelled itself "GPT-6" in the report header.

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR2-X1-1 | blocker | behaviour preservation | `TimeDepositCalculator.java:38` | A null plan type used to throw `NullPointerException` after day 30 and abort the batch; now it gets no interest and later deposits are still updated | won't fix | Developer's decision (option A): null is invalid data, excluded by the `NOT NULL` column; reproducing an accidental NPE would carry a legacy defect into the new design. Documented in `AnnualRateInterestPolicy`, pinned by `InterestPolicyTest`, listed in the README assumptions |
| PR2-X1-2 | minor | tests | `TimeDepositCalculatorTest.java:72` | No negative balance case: skipping interest on negative balances would pass the suite | valid | `7b9e076` |
| PR2-X1-3 | minor | tests | `CustomInterestPolicyTest.java:60` | Both matching policies return positive interest, so "first non-zero wins" would also pass | valid | `8debe42` |

Notes: the fix for PR2-X1-2 adds a test to the characterization test file after its commit. Existing rows are
untouched; the new test pins original behaviour, so it was added on purpose as a review fix.
Codex found the null plan type issue on its own and described it more precisely than the pre-check
(the aborted batch).

## PR 3 - Phases 4-7: ports, PostgreSQL persistence, REST API, application

- Plan phase / steps: Phases 4-7, steps 12-24 (prompt: `docs/ai/prompts/pr3-phases-4-7.md`)
- Author: Codex (GPT-6.2-Sol)
- Review mode: regular (one reviewer)
- Date: 2026-11-28

### Run 1 - Claude (chat, new session; Claude Opus 5.5)

The `/review-pr` skill exists only in Claude Code, so the reviewer read `docs/ai/prompts/review.md` from the
repository and followed it. It could not run the tests (JDK 11 only, no Maven Central access); the author ran
`./mvnw verify` with Docker after every fix commit.

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR3-C1-1 | major | dead code | `TimeDepositApplicationIT.java:29` | `@ActiveProfiles("test")` activates a profile with no configuration | valid | `dbec8f0` |
| PR3-C1-2 | major | design | `TimeDepositConfiguration.java:24` | Default policy set defined twice (Spring beans and the no-arg calculator); bean order decides "first policy wins" | valid | Calculator bean is `new TimeDepositCalculator()`, policy beans removed: `c4efaef` |
| PR3-C1-3 | minor | tests | `JdbcTimeDepositRepository.java:36` | `FOR NO KEY UPDATE` lock and the "no lost interest" claim are untested | valid | Deterministic IT: PostgreSQL reports the second update waiting on the first (`pg_blocking_pids`), final balance has exactly two months of interest: `f011487` |
| PR3-C1-4 | minor | documentation | `java/pom.xml:25` | Outdated comment about modules added in PR 3 | valid | `4257998` |
| PR3-C1-5 | minor | documentation | `docs/PLAN.md:140` | Plan does not match the code (demo seed location, `compose.yaml`) | valid | `5bc173a` |
| PR3-C1-6 | minor | commits | commit `576987f` | App wiring committed without a test; the E2E test follows in `5b18bbc` | won't fix | Would need a history rewrite; both commits are in the same PR |
| PR3-C1-7 | nit | design | `JdbcTimeDepositRepository.java:47` | One `UPDATE` per deposit instead of a batch | won't fix | Monthly job on few rows; a batch adds code without a measurable benefit here |
| PR3-C1-8 | nit | design | `TimeDepositWithWithdrawals.java:5` | Record equality depends on `TimeDeposit` identity | won't fix | The service test checks that the repository's objects are passed through unchanged, so identity is the intended comparison |
| PR3-C1-9 | nit | build | `time-deposit-adapter-rest/pom.xml:19` | Tool versions hardcoded in the module; redundant `useSpringBoot3=false` | valid | `eb27933` |
| PR3-C1-10 | nit | design | `V1__create_time_deposit_tables.sql:10` | No index on `withdrawals("timeDepositId")` used by the read join | valid | Added to V1 (not deployed anywhere yet): `36e2647` |

Notes: editing V1 changes its Flyway checksum; a local database created before `36e2647` has to be recreated
(`docker compose down -v`). The reviewer found two real AI artifacts (an unused test profile, duplicated wiring)
that the author's own self-check missed.
