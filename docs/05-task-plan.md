# 05 — Task Plan and Agent Prompts

Maps to the proposal's five milestones. M1 (proposal and design) is done. Work M2 → M5 in order; tasks inside a milestone are listed in dependency order. Each task has acceptance criteria and a ready-to-paste agent prompt.

**How to use with an AI agent:** start every session with: *"Read `AGENTS.md`, then the docs listed in the task. Do only this task. Follow the acceptance criteria. Log deviations in `docs/DECISIONS.md`."* Then paste the task prompt.

---

## Milestone 2 — Backend core OOP models

### T2.0 Project scaffold
- Spring Boot 3, Java 21, Maven. Dependencies: web, data-jpa, validation, postgresql, flyway-core (plus flyway-database-postgresql), actuator, test starter, Testcontainers PostgreSQL.
- Package skeleton from `AGENTS.md` §5. `application.yml` per `03-database-schema.md` §3. `.env.example`, `.gitignore` (excludes `.env`).
- **Accept:** `mvn verify` passes; app boots against a Testcontainers or Supabase DB with an empty schema.

> **Prompt:** Read AGENTS.md and docs/03-database-schema.md §3. Scaffold the Spring Boot backend in `backend/` with the dependencies, package skeleton, `application.yml` and `.env.example` described. Add a smoke test that boots the context. Do not create any domain classes yet.

### T2.1 Personnel hierarchy
- `Person`, `Customer`, `Employee`, `Mechanic`, `Manager` exactly per `02-domain-model.md` §3. Domain exceptions per §8.
- **Accept:** unit tests cover: abstract classes cannot be instantiated (compile-level, so test via subclass use); salary fixtures (1150 and 2300); `redeemPoints` true, false and invalid; `logOvertime` rejects `<= 0`; `registerVehicle` rejects duplicates; `Mechanic.completeServiceTask` rejects a task assigned to someone else (add after T2.4).

> **Prompt:** Read AGENTS.md and docs/02-domain-model.md §3, §8. Implement the personnel hierarchy in `com.autocare.domain` with JPA annotations per docs/03 §4. Hand-written getters, private/protected fields, validation in mutators, Javadoc naming the OOP concept. Write JUnit tests in `domain/` that do not start Spring.

### T2.2 Vehicle hierarchy
- `Vehicle`, `Car`, `Van`, `Motorcycle`, with fee constants 50, 85, 30.
- **Accept:** tests call `getBaseServiceFee()` through a `List<Vehicle>` and assert 50/85/30; `getVehicleType()` values; constructor validation (positive seats, volume, cc).

> **Prompt:** Read AGENTS.md and docs/02-domain-model.md §4. Implement the vehicle hierarchy. The fee must come from each subclass's override, with no `instanceof`, no switch. Add a `@PrePersist` that copies the fee into the `baseServiceFee` column field. Tests as described.

### T2.3 InventoryItem
- **Accept:** `deductStock` true, false when too large, false when `<= 0`, never negative after any sequence; `restock` updates `lastRestockedAt`; `isLowStock` boundary at equal-to-threshold; optional `@Version`.

> **Prompt:** Read AGENTS.md and docs/02-domain-model.md §5 (InventoryItem). Implement it with the stock invariant. Use parameterised JUnit tests, including a property-style loop of random deduct/restock calls asserting stock never drops below zero.

### T2.4 ServiceTask and TaskUsedPart
- **Accept:** status transitions allow only the two forward moves and throw on all others (parameterised matrix of 9 pairs); `addUsedPart` deducts stock and throws `InsufficientStockException` without changing stock; same part twice merges; snapshot price retained after item price edit; cost = parts + labour; no mutations after COMPLETED.

> **Prompt:** Read AGENTS.md and docs/02-domain-model.md §5 (ServiceTask, TaskUsedPart) and §9. Implement both classes. `TaskUsedPart` mutators are package-private. Write the transition matrix test and the stock-rollback test.

### T2.5 Billable and Invoice
- **Accept:** worked example in `02-domain-model.md` §10 yields 93.31; both `applyDiscount` overloads behave per `01-requirements.md` §3 (including invalid inputs); `issue()` requires all tasks COMPLETED and freezes totals; `addTask` after issue throws; receipt text contains every line item and the total; `Invoice` is usable as a `Billable` in a `List<Billable>`.

> **Prompt:** Read AGENTS.md, docs/01-requirements.md §2 M5 and §3, docs/02-domain-model.md §6, §9, §10. Implement `Billable` and `Invoice` with both overloads. Configurable values (coupons, caps) are passed in via a small immutable `BillingPolicy` value object so the domain stays Spring-free. Write the worked-example test first.

**M2 exit check:** all domain tests pass without Spring or a database; a reviewer can open each class and see the OOP concept in Javadoc.

---

## Milestone 3 — Database and persistence layer

### T3.1 Flyway schema
- Create `V1__init_schema.sql` from `03-database-schema.md` §2 verbatim, then apply to Supabase.
- **Accept:** `flyway migrate` succeeds; `ddl-auto=validate` boots; RLS enabled on every table.

> **Prompt:** Read AGENTS.md and docs/03-database-schema.md. Create `V1__init_schema.sql` exactly as in §2. Add a Testcontainers-based test that migrates a fresh Postgres and boots the JPA context with `validate`. Do not edit the SQL beyond fixing genuine errors; log any fix in DECISIONS.md.

### T3.2 Repositories
- Interfaces per §5; Testcontainers `@DataJpaTest` for each.
- **Accept:** polymorphic load check (`Car` returned for CAR rows); low-stock query; FK violation on deleting a customer with vehicles; snapshot price independence; CHECK violation on negative stock via native SQL.

> **Prompt:** Read AGENTS.md and docs/03-database-schema.md §4, §5, §7. Implement repositories and a `@DataJpaTest` suite on Testcontainers that proves each acceptance check in §7.

### T3.3 Dev seed data
- `V2__seed_dev_data.sql` in a `dev`-only location per §6.
- **Accept:** `dev` profile boots with data; default profile does not load it.

> **Prompt:** Read docs/03-database-schema.md §6. Add the seed migration under `db/dev-seed/` and wire `application-dev.yml` to include that location. Seed values must satisfy every CHECK constraint.

### T3.4 Supabase hardening
- Confirm RLS enabled, no anon policies; document pooler choice and env variables in `README`; confirm no credentials in git history.
- **Accept:** a request using the project's anon key against the Supabase REST endpoint returns no rows from any table.

---

## Milestone 4 — React + TypeScript UI (and the API it needs)

### T4.1 Services, DTOs, mappers
- DTO records and mappers per `04-api-contract.md` §4; factories per §5; services per §6, all `@Transactional`.
- **Accept:** Mockito service tests per module; `TaskService.addPart` rolls back stock on failure (integration test).

> **Prompt:** Read AGENTS.md and docs/04-api-contract.md §4–§6 plus docs/02-domain-model.md. Implement DTOs, mappers, factories and services. Services orchestrate and call domain methods; they contain no business rules that belong in the domain.

### T4.2 Controllers and error handling
- Endpoints per §3, validation, `@RestControllerAdvice` per §2.
- **Accept:** `MockMvc` or `@SpringBootTest` tests: one success and one failure per endpoint; every error code in §2 reachable; OpenAPI optional (springdoc) if time allows.

> **Prompt:** Read docs/04-api-contract.md §1–§3. Implement controllers and the global exception handler. Return exactly the error shape in §2. Write endpoint tests including every error code.

### T4.3 Frontend scaffold and types
- Vite + React + TS strict; `src/types/index.ts` copied from §4; API client and per-module API files.
- **Accept:** `npm run build` and `npm run lint` pass; no `any`.

> **Prompt:** Read AGENTS.md and docs/04-api-contract.md §4, §7. Scaffold `frontend/` with Vite React TypeScript (strict). Create `types/index.ts` exactly as in §4, the fetch client, and typed API modules for all six resources.

### T4.4 Pages (build in this order)
1. Customers list/detail with vehicle registration form (conditional fields by type).
2. Inventory table with low-stock highlighting and restock action.
3. Employees list/create, showing computed monthly salary and an overtime action.
4. Task board by status; create task from invoice; add parts; log labour; start/complete.
5. Invoice detail: discounts (both kinds), tax, issue, settle, printable receipt.
6. Dashboard: open tasks count, low-stock count, unpaid invoices.
- **Accept (each):** works against the real backend; loading and error states shown from `IApiError.message`; tablet layout at 768px.

> **Prompt (repeat per page):** Read docs/04-api-contract.md §3 for the endpoints involved and §4 for types. Build the `<Page>` page and its components using only the typed API modules. Show server error messages. No new types outside `types/index.ts`.

---

## Milestone 5 — Verification and final defence

### T5.1 End-to-end scenarios (Testcontainers + MockMvc, or Playwright)
1. Register customer + Car → open invoice → create task → add part → labour → start → complete → issue → settle: totals match hand calculation; customer gains points; stock reduced.
2. Insufficient stock returns 409 and leaves stock and task unchanged.
3. Two concurrent `addPart` calls on a part with stock for only one: exactly one succeeds (needs `@Version` or pessimistic locking).
4. Percentage discount above cap without manager returns 422; with a valid manager succeeds.
5. Delete customer with vehicles returns 409.

> **Prompt:** Read docs/05-task-plan.md T5.1 and docs/04-api-contract.md. Write the five scenarios as automated tests. Report any failing scenario with the cause instead of altering the expectations.

### T5.2 OOP compliance review (peer review checklist)
- [ ] No public setters on guarded state; no Lombok on domain classes
- [ ] No `instanceof` or type-switch in billing or salary code
- [ ] `Person`, `Employee`, `Vehicle` abstract; `Billable` is an interface implemented by `Invoice`
- [ ] Both `applyDiscount` overloads present and used
- [ ] Controllers contain no repository calls; entities never serialised directly
- [ ] TS interfaces match DTO records field for field
- [ ] Every class in the UML exists with the documented members

> **Prompt:** Act as a strict OOP reviewer. Read AGENTS.md §4 and docs/02-domain-model.md. Audit the codebase against the checklist in docs/05-task-plan.md T5.2. Output a table of findings with file and line, severity, and a suggested fix. Do not change code.

### T5.3 Documentation and viva pack
- `README.md` (setup, env vars, run, test), a class-to-OOP-concept table, UML and ERD images committed under `docs/diagrams/`, a short demo script following scenario 1, and a list of likely viva questions with answers pointing at code locations.

> **Prompt:** Read all docs plus the code. Produce `README.md`, `docs/VIVA.md` (20 likely questions with answers that cite file paths and method names), and a 5-minute demo script. Facts must be verified against the code, not assumed.

---

## Appendix A — `docs/DECISIONS.md` starter

```markdown
# Decisions log
Format: date | decision | reason | who approved

- Money: BigDecimal internally, double in public signatures (UML fidelity) | avoids float drift | team
- Added UNIQUE(task_id, part_code) on task_used_parts | matches Map<InventoryItem,Integer> | team
- TaskStatus stored as enum, exposed as String | UML says String; enum is safer | team
```

## Appendix B — Assumptions to confirm with the team and lecturer

1. Labour rate, default tax rate, loyalty ratios and coupon list (`01-requirements.md` §3) are invented defaults. Confirm or replace them.
2. Whether `ServiceTask.setStatus(String)` must literally exist or whether the guarded enum transition methods satisfy the rubric. The plan keeps both.
3. Whether Spring Boot with Spring Data JPA is acceptable as the "Java" backend (the proposal itself names Spring Data JPA, so this should be fine).
4. The UML omits `billableHours`, `laborCharge`, `partsCost` and `TaskUsedPart`; the ERD requires them. The plan adds them to the Java model. Consider updating the UML before submission.

## Appendix C — Suggested parallel work split

| Track | Tasks | Needs |
|---|---|---|
| A Domain | T2.1–T2.5 | none |
| B Database | T3.1, T3.3, T3.4 | none (schema is fixed in docs/03) |
| C Frontend | T4.3, then pages against mocked data | `types/index.ts` from docs/04 |
| D Integration | T3.2, T4.1, T4.2, then T5.x | A and B |

Tracks A, B and C can start at the same time because the contracts (UML, schema, API) are already pinned in these docs.
