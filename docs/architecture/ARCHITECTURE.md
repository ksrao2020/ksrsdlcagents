# Architecture blueprint (template for every user story)

This is the standard every story follows. The `customer` context under `src/main/java/com/acme/app/customer`
is the working reference implementation. When in doubt, copy its shape.

## 1. Layers and dependency rule

```
                 ┌──────────────────────── adapter.in.web ────────────────────────┐
  HTTP ─► OpenAPI-generated interface ◄─ Controller ─► WebMapper (MapStruct)        │
                 └───────────────────────────────┬────────────────────────────────┘
                                                 ▼ (port.in use-case interfaces)
                 ┌──────────────────────── application ───────────────────────────┐
                 │ Command/Query DTOs (records, @Builder, Jakarta validation)      │
                 │ CommandService (@Transactional)  QueryService (readOnly = true) │
                 └───────────────────────────────┬────────────────────────────────┘
                                                 ▼ (port.out interfaces)
                 ┌──────────────────────── domain ────────────────────────────────┐
                 │ Aggregates / value objects (@Builder, invariants), exceptions   │
                 └────────────────────────────────────────────────────────────────┘
                 ┌──────────────────────── adapter.out.persistence ───────────────┐
                 │ PersistenceAdapter implements port.out ─► PersistenceMapper     │
                 │ JPA Entity (@Builder, @Version) ◄─ Spring Data JpaRepository    │
                 └────────────────────────────────────────────────────────────────┘
```

Dependencies point inward only: `adapter → application → domain`. ArchUnit (`ArchitectureTest`) fails the
build when this is violated.

## 2. Package template (per bounded context)

```
com.acme.app.<context>
├── domain
│   ├── model/<Aggregate>.java                     # @Builder(toBuilder = true), @Getter, invariants
│   └── exception/<Aggregate>NotFoundException.java  # extends NotFoundException / ConflictException /
│                                                    #   BusinessRuleViolationException
├── application
│   ├── dto/<Action><Aggregate>Command.java         # record + @Builder + jakarta.validation
│   ├── mapper/<Aggregate>CommandMapper.java          # MapStruct: Command → Domain (via the aggregate builder)
│   ├── port/in/<Action><Aggregate>UseCase.java      # one interface per use case
│   ├── port/in/<Aggregate>Query.java                # read use cases
│   ├── port/out/<Aggregate>RepositoryPort.java      # persistence port (domain types only)
│   └── service/<Aggregate>CommandService.java       # @Service @Validated @Transactional
│       service/<Aggregate>QueryService.java         # @Service @Transactional(readOnly = true)
└── adapter
    ├── in/web/<Aggregate>Controller.java            # implements generated <Tag>Api, @PreAuthorize
    ├── in/web/<Aggregate>WebMapper.java             # Request→Command, Domain→Response
    └── out/persistence/
        ├── <Aggregate>Entity.java                   # @Entity, @Builder, @Version, sequence id
        ├── <Aggregate>JpaRepository.java            # Spring Data, package-private use
        ├── <Aggregate>PersistenceMapper.java        # Domain↔Entity
        └── <Aggregate>PersistenceAdapter.java       # implements RepositoryPort
```

Cross-cutting code lives in `com.acme.app.shared` (base exceptions, `GlobalExceptionHandler`, security config,
MapStruct central config).

## 3. Mapping chain (MapStruct only)

| Hop | Mapper | Direction |
|---|---|---|
| API request → Command DTO | `<Aggregate>WebMapper` | inbound |
| Command DTO → Domain model | `<Aggregate>CommandMapper` (application layer) | inbound |
| Domain model → JPA entity | `<Aggregate>PersistenceMapper` | outbound |
| JPA entity → Domain model | `<Aggregate>PersistenceMapper` | outbound |
| Domain model → API response | `<Aggregate>WebMapper` | inbound |

Rules:
- Every mapper uses `@Mapper(config = CentralMapperConfig.class)`. That config sets
  `unmappedTargetPolicy = ERROR`, so a missing field fails the build instead of being silently dropped.
- MapStruct uses the Lombok builders automatically (`lombok-mapstruct-binding` is configured).
- Domain models put `@Builder(toBuilder = true)` on a **private constructor that checks the invariants**, so a
  MapStruct-built, `toBuilder()`-copied or database-rehydrated aggregate is always valid.
- Fields that must not be mapped (id on create, `version`, audit fields) are ignored explicitly with
  `@Mapping(target = "...", ignore = true)` and a reason in a comment.
- Generated OpenAPI models never cross into `application` or `domain`. Entities never leave
  `adapter.out.persistence`.

## 4. Transactions

- Query services: class-level `@Transactional(readOnly = true)`. This lets Hibernate skip dirty checking and
  lets the Oracle driver route to read replicas where configured.
- Command services: class-level `@Transactional` (read-write, rollback on any `RuntimeException`).
- A command that also reads (e.g. load-then-update) runs entirely inside the write transaction.
- Optimistic locking: every entity has `@Version`. `ObjectOptimisticLockingFailureException` is mapped to
  **409 CONFLICT** by the global handler.
- `@EnableMethodSecurity(proxyTargetClass = true)` is required: a JDK proxy would hide `@RestController` on
  controllers that implement generated interfaces, and their routes would return 404.
- Never call a `@Transactional` method on `this` (self-invocation bypasses the proxy). Split into two beans.
- `spring.jpa.open-in-view=false`. Lazy associations are fetched explicitly in the query service.

## 5. Validation

| Where | What | How |
|---|---|---|
| API boundary | Shape: required, length, pattern, format | Constraints in `api.yaml`; generator emits Jakarta annotations; controller parameters are `@Valid` |
| Application | Command completeness | Jakarta annotations on the command record; service is `@Validated` and parameters are `@Valid` |
| Domain | Business invariants | Checked in the aggregate's factory/behaviour methods; throws `BusinessRuleViolationException` |
| Database | Last line of defence | NOT NULL / UNIQUE / CHECK constraints in Flyway migrations |

## 6. Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) is the only place that builds error responses. It returns
RFC 9457 `application/problem+json` with the properties `errorCode`, `timestamp` and (for validation)
`violations[]`.

| Exception | HTTP | errorCode |
|---|---|---|
| `NotFoundException` (domain) | 404 | `<CONTEXT>_NOT_FOUND` (from the exception) |
| `ConflictException` (domain) | 409 | from the exception |
| `BusinessRuleViolationException` (domain) | 422 | from the exception |
| `MethodArgumentNotValidException`, `ConstraintViolationException`, `HandlerMethodValidationException` | 400 | `VALIDATION_FAILED` |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_REQUEST` |
| `AccessDeniedException` / `AuthorizationDeniedException` | 403 | `ACCESS_DENIED` |
| `ObjectOptimisticLockingFailureException` | 409 | `CONCURRENT_MODIFICATION` |
| `DataIntegrityViolationException` | 409 | `DATA_INTEGRITY_VIOLATION` |
| Anything else | 500 | `INTERNAL_ERROR` (logged with a correlation id; no message leaked) |

New stories add **domain exception subclasses**, not new handler methods, unless a new HTTP status is needed.

## 7. OpenAPI

- Contract-first: `src/main/resources/openapi/api.yaml` is the source of truth, reviewed in the PR.
- `openapi-generator-maven-plugin` (spring, `interfaceOnly`, `useSpringBoot3`, `useBeanValidation`,
  `useTags`) generates the API interfaces and request/response models into `target/generated-sources`.
- Every operation declares `security` scopes, `400/401/403/404/409/422/500` responses using the shared
  `Problem` schema, and field constraints (`maxLength`, `pattern`, `format`).

## 8. Per-story checklist (the agent ticks this in the PR description)

- [ ] `.feature` file approved and committed
- [ ] `api.yaml` updated first; generated interface implemented
- [ ] Domain model with `@Builder` + invariants + unit tests
- [ ] Command/query DTOs as records with `@Builder` + Jakarta constraints
- [ ] Use-case port(s) + service with correct `@Transactional` (readOnly for queries)
- [ ] Repository port + persistence adapter + entity with `@Version` + Flyway migration
- [ ] MapStruct mappers for every hop, each with a unit test
- [ ] Domain exceptions only; no new handler methods unless a new status code is needed
- [ ] `@PreAuthorize` scopes + 401/403 tests
- [ ] `ArchitectureTest` green, coverage ≥ 80%, Sonar 0 vulnerabilities
