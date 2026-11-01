# 01 — Requirements

Source: Project Proposal §1, §3 and the Presentation (Problem Statement, Scope slides).

## 1. Problem being solved

Garages that rely on paper logs or spreadsheets suffer from:

- data silos and lost records
- spare-part stockouts during active repairs
- opaque job progress for staff and customers
- billing errors from disjointed labour, parts, tax and discounts
- no audit trail

AutoCare answers each with a modelled domain, guarded state, polymorphic calculation, and a decoupled three-tier design.

## 2. Functional modules

Each requirement has an ID so tasks and tests can reference it.

### Module 1 — Customer & Vehicle Registry

| ID | Requirement |
|---|---|
| M1-1 | Create, read, update and delete customers (name, phone, email unique, loyalty points) |
| M1-2 | Register multiple vehicles per customer; vehicle types are Car, Van, Motorcycle |
| M1-3 | A vehicle is identified by licence plate (unique) and always has exactly one owner |
| M1-4 | View a customer's full service history (invoices and their tasks, newest first) |
| M1-5 | Loyalty points: `addLoyaltyPoints` on invoice settlement, `redeemPoints` returns `false` if balance is insufficient |

### Module 2 — Mechanic & Task Dispatching

| ID | Requirement |
|---|---|
| M2-1 | Manage staff of two roles: Mechanic and Manager |
| M2-2 | Create a service task (job card) and assign it to a mechanic, preferably matching specialisation |
| M2-3 | Task status is `PENDING`, `IN_PROGRESS` or `COMPLETED`; transitions are one-way and guarded |
| M2-4 | Mechanics log overtime hours (`logOvertime`); salary is computed polymorphically |
| M2-5 | A mechanic completes a task through `completeServiceTask(task)` |
| M2-6 | List tasks filtered by status and by mechanic |

### Module 3 — Inventory & Spare Parts Control

| ID | Requirement |
|---|---|
| M3-1 | CRUD for inventory items keyed by `partCode` |
| M3-2 | `deductStock(qty)` validates availability and returns `false` (or the service raises a 409) rather than going negative |
| M3-3 | `restock(qty)` increases stock and sets `lastRestockedAt` |
| M3-4 | `isLowStock()` is true when `stockQuantity <= reorderThreshold`; a low-stock listing endpoint exists |
| M3-5 | A part is validated for availability before it is attached to a task |

### Module 4 — Workstation Diagnostic Tracking

| ID | Requirement |
|---|---|
| M4-1 | A task records a description and diagnostic findings (the `description` column holds the findings text) |
| M4-2 | Parts used on a task are itemised (`TaskUsedPart`) with quantity and a **unit-price snapshot** taken at attach time |
| M4-3 | Billable technician hours are recorded per task (`billableHours`) |
| M4-4 | `calculateTaskCost()` = parts cost + labour charge, recalculated whenever parts or hours change |

### Module 5 — Dynamic Billing & Invoicing Engine

| ID | Requirement |
|---|---|
| M5-1 | An invoice belongs to one customer and one vehicle and aggregates its tasks (composition: tasks are deleted with the invoice) |
| M5-2 | `subtotal = vehicle.getBaseServiceFee() + Σ task.calculateTaskCost()` |
| M5-3 | `total = (subtotal − discountAmount) + tax`, where `tax = (subtotal − discountAmount) × taxRate / 100` |
| M5-4 | Two overloaded discount methods: `applyDiscount(double percentage)` and `applyDiscount(String couponCode, double fixedReduction)` |
| M5-5 | A Manager approves special discounts via `approveDiscount(invoice, rate)` |
| M5-6 | `generateReceipt()` returns a formatted, itemised text breakdown |
| M5-7 | Settling an invoice sets `paymentStatus=PAID`, `settledAt`, and credits customer loyalty points |

## 3. Business rules and assumptions

The proposal leaves some numbers open. These defaults make the system buildable. All are configurable in `application.yml` under `autocare.*`, and any change goes in `docs/DECISIONS.md`.

| Topic | Default |
|---|---|
| Base service fees | Car 50.00, Van 85.00, Motorcycle 30.00 (from the proposal) |
| Labour rate | `autocare.billing.labor-rate-per-hour: 20.00` → `laborCharge = billableHours × rate` |
| Default tax rate | `autocare.billing.default-tax-rate: 0.00` (percent), overridable per invoice |
| Percentage discount | Must be `0 < pct <= 100`; `discountAmount = subtotal × pct / 100`; replaces any previous discount (does not stack) |
| Coupon discount | Coupon code must exist in `autocare.billing.coupons` (map of code → max fixed reduction); reduction must be `> 0` and `<= max` and `<= subtotal` |
| Discount ceiling | Without manager approval, percentage discounts are capped at `autocare.billing.max-unapproved-discount-pct: 10`; above that a `managerId` is required |
| Loyalty earning | 1 point per 10.00 of settled total, rounded down (`autocare.loyalty.points-per-currency-unit: 0.1`) |
| Loyalty redemption | 1 point = 0.10 off; applied via `redeemPoints(points)` then `applyDiscount` flow in the service |
| Invoice lifecycle | `OPEN` (tasks may be added) → `ISSUED` (totals frozen, receipt available) → `PAID` |
| Editing rules | Tasks and parts can be changed only while the invoice is `OPEN`. Totals recompute on every change |
| Deletion rules | A customer with vehicles or invoices cannot be deleted (FK `RESTRICT`, returns 409) |
| Currency | Displayed as `$` to match the proposal |
| Authentication | Out of scope for this coursework. Manager actions take a `managerId` parameter and verify the employee is a Manager |

## 4. Out of scope

User login and authorisation, payment gateways, email and SMS sending, multi-branch support, and any direct frontend-to-Supabase access.

## 5. Non-functional requirements

- **Type safety:** TS interfaces mirror Java DTOs one-to-one.
- **Integrity:** FK constraints with `ON DELETE RESTRICT`; check constraints for non-negative stock and valid enums.
- **Auditability:** `createdAt`, `updatedAt`, `issuedAt`, `settledAt` timestamps; unit-price snapshots on used parts; frozen totals once issued.
- **Responsiveness:** UI usable on desktop and workshop tablets (min width 768px).
- **Testability:** domain classes have no Spring dependencies, so they run in plain JUnit.
