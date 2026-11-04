#!/usr/bin/env bash
# Runs /story with usage tracking, then prints the story's usage by agent.
#
#   scripts/story-run.sh PROJ-123
#   scripts/story-run.sh PROJ-123 --permission-mode acceptEdits -p     (extra arguments go to claude)
#
# Telemetry is sent only if .env.telemetry exists (see telemetry.env.example). The local
# report works either way, because it reads Claude Code's own transcripts.
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
story=${1:-}
if [[ ! "$story" =~ ^[A-Z][A-Z0-9]+-[0-9]+$ ]]; then
  echo "Usage: scripts/story-run.sh <JIRA-ID> [claude arguments]" >&2
  exit 2
fi
shift
cd "$root"

if [[ -f .env.telemetry ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.telemetry
  set +a
  export CLAUDE_CODE_ENABLE_TELEMETRY=1
  export OTEL_RESOURCE_ATTRIBUTES="jira.id=${story}${OTEL_RESOURCE_ATTRIBUTES:+,$OTEL_RESOURCE_ATTRIBUTES}"

  # Before v2.1.273, token and cost metrics show custom agents as "custom".
  version=$(claude --version 2>/dev/null | grep -oE '^[0-9]+\.[0-9]+\.[0-9]+' || true)
  if [[ -n "$version" && "$(printf '%s\n2.1.273\n' "$version" | sort -V | head -1)" != "2.1.273" ]]; then
    echo "Warning: Claude Code $version reports custom agents as \"custom\" in telemetry. Update to 2.1.273 or newer." >&2
  fi
else
  echo "No .env.telemetry, so nothing is sent to a collector. The local usage report still works." >&2
fi

session_id=$(python3 -c 'import uuid; print(uuid.uuid4())')
mkdir -p .story-runs
echo "$session_id" >> ".story-runs/${story}.sessions"

status=0
claude --session-id "$session_id" "/story ${story}" "$@" || status=$?

echo >&2
python3 scripts/story-usage.py "$story" >&2 || true
exit "$status"
