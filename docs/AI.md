# AI-Assisted Development

## Tools

| Tool | Version / model | Role |
|---|---|---|
| Claude Code | Claude Opus 5.5, high effort | Author of PR 2; reviewer in the final review |
| Codex | GPT-6.2-Sol (default and xhigh reasoning) | Author of PR 3; reviewer of PR 1, PR 2 and the final review |
| Claude (chat, agent session) | Claude Opus 5.5 | Analysis of the kata, plan, harness (PR 1), implementation prompts, documentation (PR 4); separate new chat sessions reviewed PR 1 and PR 3 |

## Setup (reproducible)

Everything the agents use is committed to the repository:

| File | Purpose |
|---|---|
| `AGENTS.md` | Shared rules for all agents: constraints, architecture, commands, git workflow |
| `CLAUDE.md` | Imports `AGENTS.md`, adds Claude Code specifics |
| `.claude/agents/test-writer.md` | Subagent writing tests |
| `.claude/agents/architecture-reviewer.md` | Read-only subagent checking hexagonal boundaries and README constraints |
| `.claude/skills/review-pr/SKILL.md` | `/review-pr` — runs the shared review prompt |
| `.claude/settings.json`, `.claude/hooks/run-unit-tests.sh` | Stop hook: runs unit tests when `java/` differs from `main`; blocks finishing once on failure, then warns the developer |
| `docs/ai/prompts/review.md` | Shared review prompt, used verbatim by both tools |
| `docs/ai/review-log.md` | Every review finding with the developer's verdict |

How to run a review:

```bash
# Claude Code (inside the repo)
/review-pr main

# Codex CLI (bash)
codex exec "$(cat docs/ai/prompts/review.md)"
# Codex CLI (PowerShell) - -Encoding UTF8 matters on Windows PowerShell 5.1
codex exec (Get-Content -Raw -Encoding UTF8 docs/ai/prompts/review.md)

# Codex chat (IDE): copy the prompt and paste it into a new chat
Get-Content -Raw -Encoding UTF8 docs/ai/prompts/review.md | Set-Clipboard
```

Known limitations of the setup:

- `architecture-reviewer` is read-only by instruction only: it needs Bash for `git diff`.
- The Stop hook blocks finishing once; if tests still fail, Claude may finish and the developer gets a
  warning. This trade-off prevents an endless loop.
- Models do not identify themselves reliably in their reports (Codex wrote "GPT-6" for GPT-6.2-Sol),
  so the model names in the log are recorded by the developer.

## Workflow

Described in `docs/PLAN.md`, section "AI cross-review workflow":

1. Each PR has one author agent, working from a committed implementation prompt
   (`docs/ai/prompts/pr2-phases-1-3.md`, `docs/ai/prompts/pr3-phases-4-7.md`).
2. A different agent reviews it with the shared prompt `docs/ai/prompts/review.md`.
3. The developer judges every finding (`valid`, `partial`, `won't fix`, ...) and the author fixes the accepted ones
   in separate commits; everything is recorded in `docs/ai/review-log.md`.

The Claude Code `/review-pr` skill is only available in Claude Code; in the Claude chat app the reviewer reads
`docs/ai/prompts/review.md` from the repository directly.

## What was AI-assisted and why

Practically all code, tests and documentation were written by AI agents. The developer made the decisions,
judged every review finding, checked the riskiest parts and tested the application by hand. AI was used for
generation, testing and refactoring because it is fast at producing code and tests from a precise brief; the
human stayed responsible for what the brief says and for accepting the result.

| PR | What | AI involvement | Human involvement |
|---|---|---|---|
| 1 | Plan, harness, build upgrade (Java 21, Spring Boot 4.1, Maven Wrapper) | Drafted by Claude (chat); reviewed by Codex and a new Claude chat | Decisions (Java 21, multi-module hexagon, cross-review workflow), verdicts, commits |
| 2 | Characterization tests, domain module, `InterestPolicy` refactoring | Written by Claude Code; reviewed by Codex (xhigh) | Spot-check that the characterization tests were not changed after their commit and that their arithmetic is right; decision on null plan types |
| 3 | Ports, PostgreSQL adapter, REST API, application | Written by Codex; reviewed by a new Claude chat | Decisions: 204 for the update, one transaction, demo data only for local runs; manual test in Swagger UI |
| 4 | README, this document, final review | Drafted by Claude (chat); final review by Codex and Claude Code | Content decisions, verdicts on the final review |

## Claude vs Codex - comparison

Based on `docs/ai/review-log.md`. The sample is small (one kata, a few PRs), and the environment mattered as much
as the model: the Codex reviewer could build and run the code, while the Claude chat reviewer had only JDK 11
and no Maven Central access, so it could only read the code.

All reviews before the final review:

| Reviewer | Reviews | Findings | Valid | Partial | Won't fix |
|---|---|---|---|---|---|
| Codex (GPT-6.2-Sol) | 4 (PR 1 x3, PR 2) | 16 | 14 | 1 | 1 |
| Claude (chat, Claude Opus 5.5) | 2 (PR 1, PR 3) | 21 | 14 | 2 | 5 |

- **Codex as a reviewer** reported fewer findings with more functional weight, usually verified by running code:
  the Windows Maven Wrapper bug, two gaps in the Stop hook, and the only `blocker` - a behaviour change for
  `null` plan types.
- **Claude as a reviewer** reported more findings, strongest on consistency and AI artifacts: an unused test
  profile, a policy set defined twice, an untested concurrency claim, the mangled prompt encoding.
- **As authors**, Claude Code (PR 2) left two test gaps and no dead code; Codex (PR 3) left two AI artifacts and an
  untested claim. Both were caught by the other tool's review, which is the point of the cross-review.
- **Reasoning effort matters:** Codex at xhigh found everything it found at default effort, plus four more issues,
  and only at xhigh noticed that its own input was corrupted.
- **A human test still found something:** running the application by hand on Windows showed that the
  `--spring.profiles.active=local` argument does not survive Windows PowerShell; no AI reviewer had caught it
  (logged in `docs/ai/review-log.md`, fixed in the README).
- **Suggestions need review too:** one Codex fix did not work in PowerShell, and several findings were rejected
  with reasons (`won't fix`), e.g. history rewrites or premature optimisation.

PR 1 calibration in detail (same commit, blind, separate sessions):

| Reviewer | Findings | Valid | Partial | Won't fix | Character of findings |
|---|---|---|---|---|---|
| Codex, default | 3 | 3 | 0 | 0 | Few, functional, verified by running code |
| Codex, xhigh | 7 | 6 | 1 | 0 | All default findings + 4 new; noticed its own mangled input |
| Claude (chat, new session) | 11 | 7 | 2 | 2 | Many, mostly documentation and process consistency |

- Only Codex found the functional defects (Windows wrapper bug, Stop hook gaps).
- Higher reasoning effort changed the outcome: xhigh found everything default found and more.
- One Codex fix suggestion was wrong for the platform (`<` redirection in PowerShell) - suggestions need review too.
