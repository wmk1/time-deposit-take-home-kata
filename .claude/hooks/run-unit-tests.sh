#!/usr/bin/env bash
# Claude Code Stop hook: before Claude finishes a turn, run the fast unit test suite
# if anything under java/ changed. Exit code 2 blocks finishing and sends the failure
# back to Claude, so it has to fix the tests first.
set -u

input="$(cat)"

# Prevent an endless loop: if this hook already made Claude continue once, let it stop.
if printf '%s' "$input" | grep -Eq '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

# Nothing changed in the Java project -> nothing to verify.
if [ -z "$(git status --porcelain -- java)" ]; then
  exit 0
fi

cd java || exit 0
if [ -f ./mvnw ]; then MVN="sh ./mvnw"; else MVN="mvn"; fi

output="$($MVN -q -B test 2>&1)"
status=$?
if [ $status -ne 0 ]; then
  echo "Unit tests failed (mvn test in java/). Fix them before finishing:" >&2
  printf '%s\n' "$output" | tail -n 60 >&2
  exit 2
fi
exit 0
