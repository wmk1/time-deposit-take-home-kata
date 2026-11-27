# AI-Assisted Development

> Work in progress — this document is updated together with each phase of `docs/PLAN.md`.

## Tools

| Tool | Version / model | Role |
|---|---|---|
| Claude Code | _fill in_ | Author and reviewer (alternating with Codex) |
| Codex | GPT-6.2-Sol (default settings) | Author and reviewer (alternating with Claude Code) |
| Claude (chat, agent session) | _fill in_ | Initial analysis of the kata, implementation plan, bootstrap of this harness (Phase 0) |

## Setup (reproducible)

Everything the agents use is committed to the repository:

| File | Purpose |
|---|---|
| `AGENTS.md` | Shared rules for all agents: constraints, architecture, commands, git workflow |
| `CLAUDE.md` | Imports `AGENTS.md`, adds Claude Code specifics |
| `.claude/agents/test-writer.md` | Subagent writing tests |
| `.claude/agents/architecture-reviewer.md` | Read-only subagent checking hexagonal boundaries and README constraints |
| `.claude/skills/review-pr/SKILL.md` | `/review-pr` — runs the shared review prompt |
| `.claude/settings.json`, `.claude/hooks/run-unit-tests.sh` | Stop hook: unit tests must pass before Claude finishes |
| `docs/ai/prompts/review.md` | Shared review prompt, used verbatim by both tools |
| `docs/ai/review-log.md` | Every review finding with the developer's verdict |

How to run a review:

```bash
# Claude Code (inside the repo)
/review-pr main

# Codex CLI (bash)
codex exec "$(cat docs/ai/prompts/review.md)"
# Codex CLI (PowerShell)
codex exec (Get-Content -Raw docs/ai/prompts/review.md)
```

## Workflow

Described in `docs/PLAN.md`, section "AI cross-review workflow": the author agent alternates per PR,
the other agent reviews with the shared prompt, the developer judges every finding and logs it.

## What was AI-assisted and why

_Filled in per phase._

| Phase | What | AI involvement | Human involvement |
|---|---|---|---|
| 0 | Plan, harness, build upgrade | Drafted by Claude (chat) | Decisions (Java 21, Spring Boot 4.1, multi-module, review workflow), review, commits |

## Claude Code vs Codex — comparison

_Filled in at the end, based on `docs/ai/review-log.md`._
