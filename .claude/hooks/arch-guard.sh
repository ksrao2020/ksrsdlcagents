#!/usr/bin/env bash
# PreToolUse(Write|Edit): fast feedback on blueprint violations before the file is written.
# ArchitectureTest is the authoritative check; this hook catches the common mistakes early.
# Exit 2 blocks the tool call and shows stderr to Claude.
set -euo pipefail

input=$(cat)
file=$(jq -r '.tool_input.file_path // empty' <<<"$input")
[[ "$file" == *.java && "$file" == */src/main/java/* ]] || exit 0

# Check the whole file as it will look after the tool call: Write sends .content, while Edit sends a
# fragment, so apply old_string → new_string to the current file.
content=$(python3 -c '
import json, sys
t = json.load(sys.stdin)["tool_input"]
if "content" in t:
    print(t["content"])
else:
    src = open(t["file_path"]).read()
    print(src.replace(t["old_string"], t["new_string"]) if t.get("replace_all") else src.replace(t["old_string"], t["new_string"], 1))
' <<<"$input")

fail() { echo "Architecture blueprint violation in $file: $1 (see docs/architecture/ARCHITECTURE.md)" >&2; exit 2; }

grep -qE '^import javax\.(persistence|validation)' <<<"$content" && fail "use jakarta.* imports, not javax.*"

if [[ "$file" == */domain/* ]]; then
  grep -qE '^import (org\.springframework|jakarta\.(persistence|validation)|com\.fasterxml|org\.hibernate|com\.acme\.app\.api)' <<<"$content" \
    && fail "domain must be framework-free (Lombok only)"
fi

if grep -qE '^[[:space:]]*@Entity\b' <<<"$content"; then
  grep -qE '^[[:space:]]*@Data\b' <<<"$content" && fail "@Data is forbidden on JPA entities; use @Getter @Setter @Builder"
  [[ "$file" == */adapter/out/persistence/* ]] || fail "entities belong in adapter.out.persistence"
fi

if grep -qE '^[[:space:]]*@Transactional\b' <<<"$content" && [[ "$file" != */application/service/* ]]; then
  fail "@Transactional is only allowed on application services"
fi

grep -qE 'BeanUtils\.copyProperties|org\.modelmapper' <<<"$content" && fail "use MapStruct mappers for all conversions"
grep -qE '^[[:space:]]*@Autowired\b' <<<"$content" && fail "use constructor injection (@RequiredArgsConstructor)"

if [[ "$file" == */adapter/in/web/* ]] && grep -qE '^[[:space:]]*@RestController\b' <<<"$content"; then
  grep -qE 'implements [A-Za-z]+Api\b' <<<"$content" || fail "controllers must implement the OpenAPI-generated *Api interface"
  grep -qE '\bcatch *\(' <<<"$content" && fail "no try/catch in controllers; throw domain exceptions"
fi

exit 0
