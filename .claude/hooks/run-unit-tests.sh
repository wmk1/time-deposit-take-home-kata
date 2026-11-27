#!/usr/bin/env bash
# Claude Code Stop hook: before Claude finishes a turn, run the fast unit test suite
# whenever the Java project differs from main (committed on this branch or uncommitted).
#
# - Tests fail on the first stop: exit 2 blocks finishing and sends the failure back to Claude.
# - Tests still fail on the follow-up stop (stop_hook_active=true): exit 1, a non-blocking error.
#   Claude may finish, so the hook cannot loop forever, but the developer sees the warning.
set -u

input="$(cat)"
retry=false
if printf '%s' "$input" | grep -Eq '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
  retry=true
fi

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

# Run when java/ has uncommitted changes, or when commits on this branch changed java/.
# Comparing with main (not only the working tree) also covers changes committed during the turn.
java_changed=false
if [ -n "$(git status --porcelain -- java)" ]; then
  java_changed=true
elif base="$(git merge-base HEAD main 2>/dev/null)"; then
  git diff --quiet "$base" HEAD -- java || java_changed=true
else
  java_changed=true   # no main branch to compare with: be safe and run the tests
fi
[ "$java_changed" = true ] || exit 0

cd java || exit 0
if [ -f ./mvnw ]; then MVN="sh ./mvnw"; else MVN="mvn"; fi

output="$($MVN -q -B test 2>&1)"
[ $? -eq 0 ] && exit 0

if [ "$retry" = true ]; then
  echo "Unit tests are STILL FAILING after one retry (mvn test in java/) - fix them before committing." >&2
  printf '%s\n' "$output" | tail -n 30 >&2
  exit 1
fi

echo "Unit tests failed (mvn test in java/). Fix them before finishing:" >&2
printf '%s\n' "$output" | tail -n 60 >&2
exit 2
