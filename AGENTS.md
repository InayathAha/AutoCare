# AutoCare — Garage & Service Management System

> Master context for AI coding agents. Read this file first, then only the docs your task needs.
> Source of truth: the submitted Project Proposal, Presentation, ERD and UML Class Diagram. If code and these docs disagree, the docs win unless a human approves a change in `docs/DECISIONS.md`.

## 1. What this project is

A coursework project for the **Object-Oriented Programming (OOP)** module. It is graded mainly on correct, visible use of OOP: encapsulation, inheritance, dynamic and static polymorphism, abstraction and interfaces. Working features matter, but design quality matters more. Every design choice should make the OOP concepts easy to point at during the viva.

AutoCare digitises a vehicle garage: customers and vehicles, mechanics and job cards, spare-parts stock, diagnostics and consumption, and itemised billing.

## 2. Tech stack (fixed, do not substitute)

| Layer | Choice | Notes |
|---|---|---|
| Backend | Java 21, Spring Boot 3.x, Maven | Layered: Controller → Service → Domain → Repository |
| Persistence | Spring Data JPA (Hibernate) + PostgreSQL JDBC | All DB access goes through the Java backend only |
| Database | Supabase (managed PostgreSQL) | Used purely as Postgres. No Supabase client SDK, no PostgREST, no Supabase Auth |
| Migrations | Flyway | `src/main/resources/db/migration` |
| Frontend | React + TypeScript (Vite) | TS interfaces mirror Java DTOs |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL) | |

## 3. Document map

| File | Read it when you are... |
|---|---|
| `docs/01-requirements.md` | Needing the five modules, functional rules and business assumptions |
| `docs/02-domain-model.md` | Writing any Java domain class, or checking OOP mapping |
| `docs/03-database-schema.md` | Writing migrations, JPA mappings or repositories |
| `docs/04-api-contract.md` | Writing controllers, DTOs, services' public methods or the React client |
| `docs/05-task-plan.md` | Picking up work: milestones, ownership, acceptance criteria, ready-made agent prompts |
| `docs/DECISIONS.md` | Recording any deviation from the docs (create it if missing) |

## 4. Non-negotiable rules

1. **Rich domain model, not anemic.** JPA entities *are* the domain classes from the UML. Business rules live inside them (for example `InventoryItem.deductStock`), never only in services or controllers.
2. **Encapsulation.** All fields `private` (or `protected` where the UML says `#`). No public setters for state guarded by invariants. No Lombok `@Data`, `@Setter` or `@AllArgsConstructor` on domain classes. Write getters by hand.
3. **Invariants are enforced in the domain**, and throw domain exceptions:
   - stock can never go negative
   - `ServiceTask` status moves only `PENDING → IN_PROGRESS → COMPLETED`
   - `Invoice` totals are never set directly; they are recomputed by `calculateSubtotal()` and `calculateTotal()`
4. **Polymorphism must be real.** `Invoice` calls `vehicle.getBaseServiceFee()` and `Employee.calculateSalary()` without `instanceof` checks or `switch` on type. Do not replace overriding with a type field plus `if` chains.
5. **Abstract classes stay abstract:** `Person`, `Employee`, `Vehicle`. `Billable` stays a pure interface.
6. **Never expose entities over REST.** Controllers speak DTOs only. TypeScript interfaces mirror the DTOs exactly.
7. **No SQL or repository calls in controllers.** Controllers call services; services call repositories and domain methods.
8. **Schema changes only via new Flyway migrations.** `spring.jpa.hibernate.ddl-auto=validate`. Never edit an applied migration.
9. **No secrets in git.** Supabase credentials come from environment variables. Commit only `.env.example`.
10. **Money uses `BigDecimal`** in implementation, scale 2, `RoundingMode.HALF_UP`. The UML shows `double`; see `docs/02-domain-model.md` §9 for how to reconcile (public method signatures keep `double`).
11. Every domain rule gets a unit test. Every endpoint gets at least one happy-path and one failure-path test.

## 5. Repository layout (target)

```
autocare/
├── AGENTS.md
├── docs/                         # these files
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/autocare/
│       │   ├── AutoCareApplication.java
│       │   ├── domain/           # entities, abstract classes, Billable, enums
│       │   ├── repository/       # Spring Data interfaces
│       │   ├── service/          # use-case orchestration, @Transactional
│       │   ├── controller/       # @RestController, DTO in/out
│       │   ├── dto/              # request/response records
│       │   ├── mapper/           # entity <-> DTO
│       │   ├── exception/        # domain + API exceptions, @RestControllerAdvice
│       │   └── config/           # CORS, billing properties
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/V1__init_schema.sql ...
│       └── test/java/com/autocare/ ...
└── frontend/
    ├── package.json
    └── src/{types,api,pages,components,hooks}/
```

## 6. Conventions

- Package root `com.autocare`. Constructor injection only.
- IDs: `String` UUIDs (`UUID.randomUUID().toString()`) except `Vehicle` (license plate) and `InventoryItem` (part code), which use natural keys.
- Time: `java.time.Instant`, stored as `TIMESTAMPTZ`.
- REST base path `/api/v1`. JSON camelCase. Errors use the shape in `docs/04-api-contract.md` §2.
- Enums persisted with `@Enumerated(EnumType.STRING)`.
- Git: feature branches `feature/<module>-<short-name>`, PRs reviewed by a teammate for OOP compliance (Milestone 5 requirement). Conventional commit messages.
- Add Javadoc to every domain class and public domain method naming the OOP concept it demonstrates, for example `/** Demonstrates dynamic polymorphism: overrides Vehicle.getBaseServiceFee(). */`. This helps the viva.

## 7. Definition of done (any task)

- Compiles, all tests pass (`mvn verify`, `npm run build`, `npm run lint`).
- Matches the UML names, signatures and visibility in `docs/02-domain-model.md`.
- Matches the schema in `docs/03-database-schema.md` (Hibernate `validate` passes against a migrated DB).
- Matches the API contract; DTO and TS interface changed together or not at all.
- No rule in §4 violated. Any deviation logged in `docs/DECISIONS.md`.

## 8. Team and ownership (from the proposal)

| Member | Reg | Ownership |
|---|---|---|
| F.H.I. Ahamed (Lead) | 242004 | Architecture and core Java OOP engine |
| A.M. Bhakeer | 242025 | Persistence layer and Supabase DB |
| M.G.P.T. Gamage | 242059 | Frontend architecture and TypeScript |
| M.F.F. Zimra | 242222 | Integration, testing and verification |

Ownership guides who reviews what. Agents may touch any layer if the task says so, but must respect the contracts between layers.
