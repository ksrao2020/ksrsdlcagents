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

## Commands

```bash
mvn verify                 # unit tests + ArchUnit + JaCoCo ≥ 80%
mvn verify -Pit            # + Testcontainers Oracle ITs + Cucumber (needs Docker)
mvn sonar:sonar -Dsonar.qualitygate.wait=true
helm lint helm/app-service
```

To adopt this kit, rename `com.acme.app` to your base package and replace the `customer` slice once you have
your first real context. Keep at least one slice as the reference.
