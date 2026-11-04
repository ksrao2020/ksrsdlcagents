# SDLC agent kit: Claude Code + Spring Boot hexagonal template

Give Claude Code a Jira ID and it delivers the story with BDD/TDD on a fixed hexagonal/DDD blueprint:

```bash
claude "/story PROJ-123"
```

## What enforces what

| Requirement | Instruction (agents) | Enforcement (can't be skipped) |
|---|---|---|
| Hexagonal / DDD layering | `CLAUDE.md`, `docs/architecture/ARCHITECTURE.md`, all agents | `ArchitectureTest` (ArchUnit) + `arch-guard.sh` hook |
| OpenAPI contract-first | `solution-architect` edits `api.yaml` first | Generated `*Api` interfaces; ArchUnit: controllers must implement them |
| MapStruct Request→DTO→Model→Entity | `implementer`, ARCHITECTURE §3 | `unmappedTargetPolicy = ERROR`; ArchUnit bans BeanUtils/ModelMapper |
| Read/write transactions | ARCHITECTURE §4 | ArchUnit: `@Transactional` only in `application.service` |
| Lombok `@Builder` | `implementer` | `arch-guard.sh` blocks `@Data` on entities |
| Global exception handling | `GlobalExceptionHandler` + `DomainException` hierarchy | ArchUnit: domain exceptions extend `DomainException` |
| Jakarta validation | API constraints in YAML, command records, domain invariants | Controller/validation tests |
| TDD | `test-writer` → `implementer` | `tdd-guard.sh` blocks prod edits without a test change |
| ≥ 80% coverage | | JaCoCo `check` (line + branch) in `mvn verify` + `quality-gate.sh` Stop hook |
| 0 Sonar vulnerabilities | `security-reviewer` | Sonar quality gate with `-Dsonar.qualitygate.wait=true` in CI |

## Layout

- `.claude/commands/story.md`: the `/story <JIRA-ID>` workflow
- `.claude/agents/`: `bdd-analyst`, `solution-architect`, `test-writer`, `implementer`, `architecture-reviewer`, `security-reviewer`
- `.claude/hooks/`: `arch-guard.sh`, `tdd-guard.sh`, `quality-gate.sh`
- `src/main/java/com/acme/app/customer`: the reference slice every story copies
- `src/main/java/com/acme/app/shared`: base exceptions, global handler, security, MapStruct config

## Running the app

The app needs Java 21 or newer.

- **On a laptop, without Oracle:** set `SPRING_PROFILES_ACTIVE=local` and run `com.acme.app.Application` from your IDE. It uses in-memory H2. Health is at `http://localhost:8080/actuator/health` and Swagger UI is at `http://localhost:8080/swagger-ui.html`. API calls, including those made from Swagger, return 401 because no token can validate.
- **Against real services:** set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `OAUTH2_ISSUER_URI`, and leave the profile unset. Startup fails if any of them is missing.

## Tracking token usage

Start stories with the runner instead of `/story`, so each session is recorded against its Jira ID:

```bash
scripts/story-run.sh PROJ-123
scripts/story-usage.py PROJ-123         # table by agent; add --json for a dashboard or CI
```

- **Local report:** needs no setup. It reads Claude Code's transcripts on the machine that ran the story, and prints estimated cost at API list prices. Update `PRICES` in the script when prices change. Copy the `--json` output somewhere durable if you need it long term, because transcripts can be cleaned up.
- **Team telemetry:** copy `telemetry.env.example` to `.env.telemetry` and set your OpenTelemetry collector. The runner tags every metric and event with `jira.id`. Use Claude Code 2.1.273 or newer, or custom agents appear as `custom` in token and cost metrics.

Both report estimates. Use your Anthropic billing data for actual spend.

## Commands

```bash
mvn verify                 # unit tests + ArchUnit + JaCoCo ≥ 80%
mvn verify -Pit            # + Testcontainers Oracle ITs + Cucumber (needs Docker)
mvn sonar:sonar -Dsonar.qualitygate.wait=true
helm lint helm/app-service
```

To adopt this kit, rename `com.acme.app` to your base package and replace the `customer` slice once you have
your first real context. Keep at least one slice as the reference.
