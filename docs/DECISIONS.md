# Decisions log
Format: date | decision | reason | who approved

- 2026-11-01 | Money: BigDecimal internally, double in public signatures (UML fidelity) | avoids float drift | team
- 2026-11-01 | Added UNIQUE(task_id, part_code) on task_used_parts | matches Map<InventoryItem,Integer> | team
- 2026-11-01 | TaskStatus stored as enum, exposed as String | UML says String; enum is safer | team
- 2026-11-02 | Configurable billing values (coupons, caps, labour rate) passed via immutable BillingPolicy value object | Keeps domain model Spring-free while allowing flexible configuration | team
- 2026-11-02 | Dev seed migration isolated in db/dev-seed with profile-specific flyway location | Prevents demo data polluting production profile | team
- 2026-11-02 | Invoices initialized with issuedAt = Instant.now() on creation | Satisfies NOT NULL schema constraint while preserving lifecycle updates | team
- 2026-11-02 | Pessimistic locking on InventoryItemRepository.findByPartCodeForUpdate | Guarantees atomic concurrent stock deduction under race conditions without modifying V1 schema for @Version | team
- 2026-11-02 | Customer deletion with vehicles or invoices throws DataIntegrityViolationException (409 FK_CONSTRAINT) | Conforms to docs/04-api-contract.md §3 and database ON DELETE RESTRICT constraint | team
