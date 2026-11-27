@AGENTS.md

## Claude Code specifics

- Subagents (`.claude/agents/`): `test-writer` (writes tests), `architecture-reviewer`
  (read-only check of hexagonal boundaries and README constraints — run it before proposing a commit).
- `/review-pr [base]` (`.claude/skills/review-pr/`) runs the shared review prompt.
- A Stop hook (`.claude/hooks/run-unit-tests.sh`) runs `mvn test` when `java/` has changes;
  if tests fail you will be asked to continue and fix them before finishing.
