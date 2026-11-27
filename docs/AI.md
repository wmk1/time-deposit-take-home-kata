# AI-Assisted Development

> Work in progress — this document is updated together with each phase of `docs/PLAN.md`.

## Tools

| Tool | Version / model | Role |
|---|---|---|
| Claude Code | first used as author in PR 2 | Author and reviewer (alternating with Codex) |
| Codex | GPT-6.2-Sol (default and xhigh reasoning) | Author and reviewer (alternating with Claude Code) |
| Claude (chat, agent session) | Claude Opus 5.5 | Initial analysis of the kata, implementation plan, bootstrap of this harness (Phase 0) |

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

Described in `docs/PLAN.md`, section "AI cross-review workflow": the author agent alternates per PR,
the other agent reviews with the shared prompt, the developer judges every finding and logs it.

## What was AI-assisted and why

_Filled in per phase._

| Phase | What | AI involvement | Human involvement |
|---|---|---|---|
| 0 | Plan, harness, build upgrade | Drafted by Claude (chat) | Decisions (Java 21, Spring Boot 4.1, multi-module, review workflow), review, commits |
| 0 | Fixes after the PR 1 reviews | Findings by Codex and Claude, fixes drafted by Claude (chat) | Verdict on every finding, rejected the history rewrite, commits |

## Claude Code vs Codex - comparison

The final comparison is written after PR 4, based on `docs/ai/review-log.md`.

Observations from the PR 1 calibration (same commit, blind, separate sessions):

| Reviewer | Findings | Valid | Partial | Won't fix | Character of findings |
|---|---|---|---|---|---|
| Codex, default | 3 | 3 | 0 | 0 | Few, functional, verified by running code |
| Codex, xhigh | 7 | 6 | 1 | 0 | All default findings + 4 new; noticed its own mangled input |
| Claude (chat, new session) | 11 | 7 | 2 | 2 | Many, mostly documentation and process consistency |

- Only Codex found the functional defects (Windows wrapper bug, Stop hook gaps).
- Higher reasoning effort changed the outcome: xhigh found everything default found and more.
- One Codex fix suggestion was wrong for the platform (`<` redirection in PowerShell) - suggestions need review too.
