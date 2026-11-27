@AGENTS.md

## Claude Code specifics

- Subagents (`.claude/agents/`): `test-writer` (writes tests), `architecture-reviewer`
  (read-only check of hexagonal boundaries and README constraints — run it before proposing a commit).
- `/review-pr [base]` (`.claude/skills/review-pr/`) runs the shared review prompt.
- A Stop hook (`.claude/hooks/run-unit-tests.sh`) runs `mvn test` whenever `java/` differs from `main`
  (committed or not). If tests fail, finishing is blocked once and the failure is sent back to you;
  if they still fail on the next attempt, you may finish but the developer gets a warning.
  Never finish with failing tests on purpose.
