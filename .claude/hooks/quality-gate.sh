#!/usr/bin/env bash
# Stop hook: Claude cannot finish while `mvn verify` (tests + ArchitectureTest + JaCoCo ≥ 80%) is red.
# Exit 2 makes Claude continue, with stderr as the reason.
set -uo pipefail

input=$(cat)
# Avoid an infinite loop: if Claude is already continuing because of this hook, let it stop.
[[ "$(jq -r '.stop_hook_active // false' <<<"$input")" == "true" ]] && exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
# Only gate when production or test code changed in this session's working tree.
git status --porcelain -- src pom.xml 2>/dev/null | grep -q . || exit 0

if ! out=$(mvn -q -B verify 2>&1); then
  {
    echo "Quality gate failed: mvn verify is red. Fix it before finishing."
    grep -E 'ERROR|FAIL|Rule .* was violated|coverage' <<<"$out" | head -40
  } >&2
  exit 2
fi
exit 0
