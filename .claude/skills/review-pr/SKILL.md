---
name: review-pr
description: Review the current branch against a base branch using the shared cross-review prompt. Use only when explicitly asked to review a PR or branch.
disable-model-invocation: true
---

Read `docs/ai/prompts/review.md` and follow it exactly. The same prompt is used by Codex,
so do not add review criteria and do not change the output format.

Base branch: `$ARGUMENTS` (use `main` when empty).
