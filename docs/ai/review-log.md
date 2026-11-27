# AI cross-review log

Every finding from an AI review is recorded here and judged by the developer.
The process is described in `docs/PLAN.md` (section "AI cross-review workflow");
the review prompt is `docs/ai/prompts/review.md`. The comparison of tools lives in `docs/AI.md`.

## Legend

- **Author / Reviewer**: `Claude Code`, `Codex`, `Claude (chat)` (+ model name).
- **Severity** (as reported by the reviewer): `blocker`, `major`, `minor`, `nit`.
- **Verdict** (developer's judgement): `valid`, `partial`, `false positive`, `duplicate`, `won't fix`.
- **Action**: commit SHA of the fix, or the reason why nothing was changed.
- **ID**: `PR<number>-<C|X>-<n>` (C = Claude Code, X = Codex).

## Summary

| PR | Author | Reviewer | Findings | Valid | Partial | False positive | Won't fix |
|---|---|---|---|---|---|---|---|
| 1 (run 1) | Claude (chat) | Codex | 3 | 3 | 0 | 0 | 0 |

---

<!-- Copy this block for each reviewed PR -->

## PR #<n> — <title>

- Plan phase / steps: 
- Author: 
- Reviewer: 
- Review mode: regular / calibration (blind, both agents)
- Date: 

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| | | | | | | |

Notes: 

## PR 1 — Phase 0: foundations and AI harness

- Plan phase / steps: Phase 0, steps 1–4
- Author: Claude (chat)
- Reviewer: Codex (GPT-6.2-Sol, default settings) — run 1
- Review mode: calibration (blind, both agents)
- Date: 2026-11-27

Run 1 reviewed an incomplete branch: the harness commits had not been made yet (finding PR1-X-2).
Both agents re-review the complete branch in run 2.

| ID | Severity | Category | Location | Finding (short) | Verdict | Action |
|---|---|---|---|---|---|---|
| PR1-X-1 | major | correctness | `java/mvnw:1` | Wrapper committed as `100644` from Windows; `./mvnw` fails with "permission denied" on Unix | valid | _commit SHA_ |
| PR1-X-2 | major | documentation | `docs/PLAN.md:83` | Harness and AI docs described in the plan are untracked, the PR would not contain them | valid | _commit SHAs_ |
| PR1-X-3 | minor | documentation | `java/pom.xml:30` | Spring Boot 4.1 manages JUnit 6.0.3, docs say JUnit 5 | valid | Docs updated to JUnit 6 instead of pinning JUnit 5 (would fight the Spring Boot BOM); _commit SHA_ |

Notes: PR1-X-1 is a Windows-specific pitfall the author missed; Codex verified it via the git file mode.
PR1-X-3 was verified by Codex against the resolved dependency version, not guessed.
