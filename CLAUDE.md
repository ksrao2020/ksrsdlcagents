# Engineering rules (non-negotiable)

Stack: Java 21, Spring Boot 3.5, Oracle (Spring Data JPA + Flyway), OAuth2 Resource Server (JWT),
OpenAPI contract-first, MapStruct, Lombok, Docker image + Helm chart deployed to Kubernetes.

The architecture is **hexagonal (ports & adapters) with DDD tactical patterns**. The full rules are in
`docs/architecture/ARCHITECTURE.md`, and the `customer` bounded context is the **golden reference slice**:
every new user story copies its structure and conventions. Read both before writing any code.

## Process: BDD outside, TDD inside
1. Every user story becomes Gherkin scenarios in `src/test/resources/features/<JIRA-ID>.feature`.
2. Never write production code without a failing test that needs it (red → green → refactor).
3. Run `mvn -q test` after each step and show that it is red or green.
4. One scenario at a time. Commit at every green, with messages like `PROJ-123: <what>`.

## Architecture (summary — details in docs/architecture/ARCHITECTURE.md)
- Package per bounded context: `com.acme.app.<context>.{domain,application,adapter}`.
- `domain` is pure Java (Lombok allowed). No Spring, JPA, Jakarta, Jackson or adapter imports.
- `application` holds ports (`port.in` use cases, `port.out` repositories/clients), command/query DTOs and
  `@Service` implementations. It never imports adapters.
- `adapter.in.web` implements the **generated** OpenAPI interface. `adapter.out.persistence` owns JPA entities.
- The mapping chain is fixed: **API request → Command DTO → Domain model → JPA entity** and back:
  **Entity → Domain model → API response**. Every hop is a MapStruct mapper. No hand-written mapping,
  no `BeanUtils.copyProperties`, and no `ModelMapper`.
- The API contract lives in `src/main/resources/openapi/api.yaml`. Change the YAML first, then regenerate
  (`mvn generate-sources`). Never hand-edit generated code.
- Transactions are declared only on application services: `@Transactional(readOnly = true)` on query
  services and `@Transactional` on command services. Never on controllers, adapters or repositories.
- Use Lombok `@Builder` (with `toBuilder = true` on domain models) for domain models, entities and DTO classes.
  Never put `@Data` on entities. Commands are Java records with `@Builder`.
- Errors go through the single `GlobalExceptionHandler`, returning RFC 9457 `ProblemDetail` with a stable
  `errorCode`. Throw domain exceptions (subclasses of `DomainException`); never return error bodies from
  controllers.
- Validation: Jakarta Bean Validation at the API boundary (generated from the OpenAPI constraints) and on
  command DTOs (`@Validated` service + `@Valid` parameter). Business invariants live in the domain model.
- `ArchitectureTest` (ArchUnit) enforces these rules. It must never be weakened or skipped.

## Testing
- Unit: JUnit 5 + Mockito + AssertJ. Web slices: `@WebMvcTest` with `jwt()` post-processors.
- Mappers: a unit test for every mapper (`Mappers.getMapper(...)`).
- Persistence: `*IT` tests with `@DataJpaTest` + Testcontainers (`gvenzl/oracle-free`). No H2.
- Acceptance: Cucumber-JVM (`*.feature` + steps), run in the `it` profile.
- Coverage: JaCoCo line AND branch ≥ 80% (enforced by `mvn verify`). No assertion-free tests.
  Only generated code and the application main class are excluded, in pom.xml.

## Security (Sonar must report 0 vulnerabilities and 0 unreviewed hotspots)
- Every endpoint is secured with `@PreAuthorize` scopes (e.g. `hasAuthority('SCOPE_customers.write')`).
- Only parameterized queries/JPA. Never concatenate strings into SQL/JPQL.
- No secrets in code or YAML. Read them from env vars or K8s Secrets / Vault.
- Never return stack traces or exception messages from infrastructure in API responses.
- Test that every endpoint returns 401 without a token and 403 with the wrong scope.

## Kubernetes
- Non-root, readOnlyRootFilesystem, resource requests/limits, liveness/readiness probes using
  actuator health groups. Manifests must pass `helm lint` and `kubeconform`.

## Definition of Done
`mvn verify` is green (unit tests + ArchUnit + JaCoCo check), `mvn verify -Pit` is green in CI, the Sonar
quality gate passes, OWASP dependency-check reports no High/Critical findings, `helm lint` passes, the
architecture checklist in docs/architecture/ARCHITECTURE.md is satisfied, a PR is opened, and Jira is updated.
