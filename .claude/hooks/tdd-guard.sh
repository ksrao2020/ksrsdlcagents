#!/usr/bin/env bash
# PreToolUse(Write|Edit): TDD guard. Production Java may only change while the branch already has a
# changed or new test file, i.e. a test was written first. Exit 2 blocks the edit.
set -euo pipefail

input=$(cat)
file=$(jq -r '.tool_input.file_path // empty' <<<"$input")
[[ "$file" == *.java && "$file" == */src/main/java/* ]] || exit 0

root=$(git -C "$(dirname "$file")" rev-parse --show-toplevel 2>/dev/null) || exit 0
base=$(git -C "$root" merge-base HEAD origin/main 2>/dev/null || git -C "$root" merge-base HEAD main 2>/dev/null || echo HEAD)

changed_tests=$(
  { git -C "$root" diff --name-only "$base" -- 'src/test/**'
    git -C "$root" ls-files --others --exclude-standard -- 'src/test/**'; } | sort -u
)

if [[ -z "$changed_tests" ]]; then
  echo "TDD guard: write a failing test (src/test/...) before changing production code ($file)." >&2
  exit 2
fi
exit 0
