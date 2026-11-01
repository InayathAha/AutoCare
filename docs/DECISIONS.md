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
- 2026-11-02 | Configurable CORS origins via CORS_ALLOWED_ORIGINS (comma-separated, defaulting to http://localhost:5173) | Enables seamless cloud deployment with separate frontend (Vercel) and backend (Render/Railway) domains | team
- 2026-11-02 | Dynamic server port binding via PORT environment variable (default 8080) and multi-stage backend Dockerfile | Standard container runtime compliance for PaaS platforms like Render and Railway | team
- 2026-11-02 | Safe path joining and explicit HTTP error reporting for non-JSON responses in frontend API client | Fixes blind 404s when deployed on static hosts like Vercel and clarifies failure root cause | team
