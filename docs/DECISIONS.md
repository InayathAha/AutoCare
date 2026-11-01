# Decisions log
Format: date | decision | reason | who approved

- 2026-11-01 | Money: BigDecimal internally, double in public signatures (UML fidelity) | avoids float drift | team
- 2026-11-01 | Added UNIQUE(task_id, part_code) on task_used_parts | matches Map<InventoryItem,Integer> | team
- 2026-11-01 | TaskStatus stored as enum, exposed as String | UML says String; enum is safer | team
- 2026-11-02 | Configurable billing values (coupons, caps, labour rate) passed via immutable BillingPolicy value object | Keeps domain model Spring-free while allowing flexible configuration | team
