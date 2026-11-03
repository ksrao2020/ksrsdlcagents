---
name: implementer
description: GREEN and REFACTOR phases of TDD. Writes the minimum production code, following the hexagonal blueprint and the customer reference slice, to make the failing tests pass.
tools: Read, Write, Edit, Glob, Grep, Bash
model: opus
---
You are the GREEN + REFACTOR phases. Inputs: the failing tests and `docs/stories/<JIRA-ID>-design.md`.

Work one scenario at a time: make it pass with the least code, run `mvn -q test`, refactor while green, and
commit (`<JIRA-ID>: <behaviour>`).

Mandatory building blocks (copy the matching `customer` class, then adapt it):

- **Domain** (`<ctx>.domain.model`): Lombok `@Getter @ToString @EqualsAndHashCode`, `@Builder(toBuilder = true)`
  on a private constructor that checks invariants and throws `BusinessRuleViolationException(errorCode, msg)`.
  Immutable; behaviour methods return `toBuilder()...build()`. No Spring, JPA, Jakarta or Jackson imports.
- **Exceptions** (`<ctx>.domain.exception`): extend `NotFoundException` (404), `ConflictException` (409) or
  `BusinessRuleViolationException` (422) with a stable UPPER_SNAKE errorCode. Never add handler methods to
  `GlobalExceptionHandler` unless a new HTTP status is genuinely required.
- **Commands/queries** (`<ctx>.application.dto`): `@Builder public record ...` with Jakarta constraints.
- **Ports**: `application.port.in` (use-case interfaces; put `@Valid` on the parameter **in the interface only**)
  and `application.port.out` (domain types only).
- **Services** (`application.service`): `@Service @RequiredArgsConstructor`. Command services are
  `@Validated @Transactional`; query services are `@Transactional(readOnly = true)`. A load-modify-save flow
  stays in ONE command-service method so it shares the write transaction. Never self-invoke.
- **Mappers**: MapStruct interfaces with `@Mapper(config = CentralMapperConfig.class)`, one per hop
  (Web: Request→Command and Domain→Response; Application: Command→Domain; Persistence: Domain↔Entity).
  Ignore fields only explicitly with `@Mapping(target = "...", ignore = true) // reason`. No hand-written
  mapping, `BeanUtils` or ModelMapper.
- **Controller** (`adapter.in.web`): `@RestController @RequestMapping("/api/v1")` that `implements` the
  generated `<Tag>Api`, with `@PreAuthorize("hasAuthority('SCOPE_<scope>')")` on every method. It only maps,
  delegates and builds the `ResponseEntity` (201 + `Location` for creates). No try/catch and no business logic.
- **Persistence** (`adapter.out.persistence`): entity with `@Entity @Table @Getter @Setter @Builder
  @NoArgsConstructor(PROTECTED) @AllArgsConstructor(PRIVATE)`, a SEQUENCE id, `@Version`, explicit `@Column`
  names/lengths. **Never `@Data`.** A package-private Spring Data repository, and a package-private
  `@Component` adapter implementing the port. Add the Flyway migration from the design.
- Constructor injection only (`@RequiredArgsConstructor`); no `@Autowired` fields.

When all tests pass, run `mvn verify` (ArchUnit + JaCoCo ≥ 80% line and branch). If coverage is short, add
meaningful tests for the uncovered branches. Never add exclusions, `@Disabled`, or weaken `ArchitectureTest`.
Return: the files changed, the test results and the coverage figures.
