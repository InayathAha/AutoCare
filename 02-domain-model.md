# 02 — Domain Model (UML → Java)

Source: Proposal §4–§5, Presentation slides 4–5, UML Class Diagram.
Package: `com.autocare.domain`. These are JPA entities **and** the OOP domain classes. Keep them free of Spring beans and service logic.

Visibility key: `#` protected, `-` private, `+` public.

## 1. Class overview (11 classes and interfaces)

```
Person (abstract)  ◄── Customer
        ▲
        └── Employee (abstract) ◄── Mechanic
                                ◄── Manager

Vehicle (abstract) ◄── Car, Van, Motorcycle

InventoryItem      ServiceTask      Billable «interface» ◄ ─ ─ Invoice
```

## 2. Relationships (from the UML)

| From | To | Kind | Multiplicity | Label |
|---|---|---|---|---|
| Customer | Vehicle | association | 1 → 0..* | owns / registers |
| Vehicle | Invoice | association | 1 → 0..* | serviced |
| Customer | Invoice | association | 1 → 0..* | billed to |
| Invoice | ServiceTask | **composition** (filled diamond) | 1 → 1..* | itemizes |
| Mechanic | ServiceTask | association | 1 → 0..* | assigned to |
| ServiceTask | InventoryItem | association | 0..* → 0..* | consumes / uses (via `TaskUsedPart`) |
| Manager | Invoice | dependency (dashed) | n/a | approves discount |
| Invoice | Billable | realisation (dashed) | n/a | implements |

## 3. Personnel hierarchy

### `Person` — abstract, `@MappedSuperclass`

| Vis | Member | Type / Returns |
|---|---|---|
| # | id | String |
| # | name | String |
| # | phone | String |
| # | email | String |
| + | getId() | String |
| + | getName() | String |
| + | getContactDetails() | String (concatenated `name, phone, email`) |
| + | displayProfile() | void, **abstract** |

Notes: Customer and Employee map to different PK column names (`customer_id`, `employee_id`), so each subclass uses `@AttributeOverride(name = "id", column = @Column(name = "..."))`.

### `Customer extends Person` — `@Entity` table `customers`

| Vis | Member | Notes |
|---|---|---|
| - | registeredVehicles : List&lt;Vehicle&gt; | `@OneToMany(mappedBy="owner")`; return an unmodifiable view from `getVehicles()` |
| - | loyaltyPoints : int | never negative |
| + | registerVehicle(Vehicle v) : void | sets `v.owner`, adds to list, rejects duplicates |
| + | getVehicles() : List&lt;Vehicle&gt; | unmodifiable |
| + | addLoyaltyPoints(int points) : void | rejects `points <= 0` |
| + | redeemPoints(int points) : boolean | `false` if `points <= 0` or exceeds balance; otherwise deduct and return `true` |
| + | displayProfile() : void | override; prints profile to stdout (also provide a `String describe()` helper for tests) |

### `Employee extends Person` — abstract, `@Entity`, single table `employees`

| Vis | Member | Notes |
|---|---|---|
| # | baseSalary : double | `>= 0` |
| # | employeeRole : String | mirrored by the discriminator |
| + | calculateSalary() : double | **abstract** |
| + | getEmployeeRole() : String | |
| + | displayProfile() : void | declared in UML; subclasses may override |

JPA: `@Inheritance(strategy = SINGLE_TABLE)`, `@DiscriminatorColumn(name = "employee_role", length = 30)`.

### `Mechanic extends Employee` — `@DiscriminatorValue("MECHANIC")`

| Vis | Member | Notes |
|---|---|---|
| - | specialization : String | Engine, Transmission, Electrical, etc. |
| - | overtimeHours : int | `>= 0` |
| - | hourlyRate : double | overtime pay rate |
| + | calculateSalary() : double | **override**: `baseSalary + overtimeHours × hourlyRate` |
| + | logOvertime(int hours) : void | rejects `hours <= 0`; increments |
| + | completeServiceTask(ServiceTask task) : void | verifies `task.assignedMechanic` is this mechanic, then calls the task's guarded transition to `COMPLETED` |

### `Manager extends Employee` — `@DiscriminatorValue("MANAGER")`

| Vis | Member | Notes |
|---|---|---|
| - | monthlyBonus : double | `>= 0` |
| + | calculateSalary() : double | **override**: `baseSalary + monthlyBonus` |
| + | approveDiscount(Invoice invoice, double rate) : void | calls `invoice.applyDiscount(rate)` (the overloaded percentage version) |
| + | generateInventoryReport() : String | text summary of low-stock items. The service passes items in, since the domain has no repository |

## 4. Vehicle hierarchy

### `Vehicle` — abstract, `@Entity`, single table `vehicles`

| Vis | Member | Notes |
|---|---|---|
| # | licensePlate : String | `@Id`, column `license_plate` |
| # | brand : String | |
| # | model : String | model designation and year |
| # | owner : Customer | `@ManyToOne(optional=false)`, column `customer_id` |
| + | getLicensePlate() : String | |
| + | getBaseServiceFee() : double | **abstract** |
| + | getVehicleType() : String | **abstract** |

JPA: `@Inheritance(strategy = SINGLE_TABLE)`, `@DiscriminatorColumn(name = "vehicle_type", length = 20)`. The `base_service_fee` column is populated from `getBaseServiceFee()` in a `@PrePersist` hook. It exists for reporting only; billing always calls the method.

| Subclass | Discriminator | Extra field | `getBaseServiceFee()` | `getVehicleType()` |
|---|---|---|---|---|
| `Car` | `CAR` | `- seatingCapacity : int` (column `seating_capacity`) | 50.00 | "Car" |
| `Van` | `VAN` | `- cargoVolume : double` (m³, column `cargo_volume`) | 85.00 | "Van" |
| `Motorcycle` | `MOTORCYCLE` | `- engineCC : int` (column `engine_cc`) | 30.00 | "Motorcycle" |

Fees should come from named constants in each subclass (for example `private static final double BASE_FEE = 50.00`), not from a lookup table. This keeps the overriding visible.

## 5. Operations classes

### `InventoryItem` — `@Entity` table `inventory_items`

| Vis | Member | Notes |
|---|---|---|
| - | partCode : String | `@Id`, column `part_code` |
| - | partName : String | |
| - | unitPrice : double | `>= 0` |
| - | stockQuantity : int | **invariant: never < 0** |
| - | reorderThreshold : int | `>= 0` |
| + | deductStock(int quantity) : boolean | `false` if `quantity <= 0` or `quantity > stockQuantity`; otherwise decrement and `true` |
| + | restock(int quantity) : void | rejects `quantity <= 0`; increments; sets `lastRestockedAt` |
| + | isLowStock() : boolean | `stockQuantity <= reorderThreshold` |
| + | getUnitPrice() : double | |
| + | getPartCode() : String | |

### `ServiceTask` — `@Entity` table `service_tasks`

| Vis | Member | Notes |
|---|---|---|
| - | taskId : String | UUID `@Id` |
| - | description : String | service type and diagnostic findings |
| - | status : String | persisted as enum `TaskStatus {PENDING, IN_PROGRESS, COMPLETED}`; accessors speak String per the UML |
| - | assignedMechanic : Mechanic | `@ManyToOne(optional=false)` |
| - | usedItems : Map&lt;InventoryItem,Integer&gt; | backed by `@OneToMany List<TaskUsedPart>`; `getUsedItems()` builds the map |
| - | billableHours : double | not in the UML; required by the ERD and M4-3 |
| + | addUsedPart(InventoryItem item, int qty) : void | requires status ≠ COMPLETED; calls `item.deductStock(qty)`, throws `InsufficientStockException` if it returns `false`; creates or merges the `TaskUsedPart` (snapshot `unitPrice`); recomputes `partsCost` |
| + | calculateTaskCost() : double | `partsCost + laborCharge`; updates `totalTaskCost` |
| + | setStatus(String s) : void | validates transition; throws `InvalidStateTransitionException`. Allowed: PENDING→IN_PROGRESS, IN_PROGRESS→COMPLETED. Anything else is rejected |
| + | getStatus() : String | |
| + | getTaskId() : String | |
| + | recordBillableHours(double hours, double ratePerHour) : void | extra helper; sets `billableHours` and `laborCharge`; only while not COMPLETED |

Cached columns `partsCost`, `laborCharge`, `totalTaskCost` are written by the domain methods above. Nothing else may set them.

### `TaskUsedPart` — `@Entity` table `task_used_parts` (junction, from the ERD)

Fields: `usedPartId` (generated Long), `task` (ManyToOne), `item` (ManyToOne, `part_code`), `quantity`, `unitPrice` (snapshot), `subtotal`, `attachedAt`. Package-private constructor and mutators so only `ServiceTask` manages it.

## 6. Billing

### `Billable` — interface

```java
public interface Billable {
    double calculateTotal();      // net payable amount
    String generateReceipt();     // formatted itemised breakdown
}
```

### `Invoice implements Billable` — `@Entity` table `invoices`

| Vis | Member | Notes |
|---|---|---|
| - | invoiceId : String | UUID `@Id` |
| - | customer : Customer | `@ManyToOne(optional=false)` |
| - | vehicle : Vehicle | `@ManyToOne(optional=false)`, column `license_plate` |
| - | taskList : List&lt;ServiceTask&gt; | `@OneToMany(mappedBy="invoice", cascade=ALL, orphanRemoval=true)` — composition |
| - | taxRate : double | percent |
| - | discountAmount : double | currency amount |
| - | status | enum `OPEN, ISSUED` in domain; DB column `payment_status` holds `OPEN, ISSUED, PAID` |
| + | calculateSubtotal() : double | `vehicle.getBaseServiceFee() + Σ task.calculateTaskCost()` |
| + | calculateTotal() : double | **Billable**: `(subtotal − discountAmount) + tax` |
| + | generateReceipt() : String | **Billable**: itemised lines: base fee, each task with its parts, subtotal, discount, tax, total |
| + | applyDiscount(double percentage) : void | **overload 1** (static polymorphism) |
| + | applyDiscount(String couponCode, double fixedReduction) : void | **overload 2**; validation rules in `01-requirements.md` §3 |
| + | addTask(ServiceTask t) : void | only while OPEN; sets back-reference |
| + | issue() : void | freezes totals, writes `subtotal`, `totalAmount`, `issuedAt`; requires at least one task and every task COMPLETED |
| + | settle() : void | requires ISSUED; sets PAID and `settledAt` |

The invoice must never expose `setTotal` or `setSubtotal`. Persisted `subtotal` and `total_amount` columns are refreshed only inside `issue()` (and on recalculation while OPEN).

## 7. OOP concept → where to see it

| Concept | Evidence in code |
|---|---|
| Encapsulation | private fields, validated mutators, `deductStock` guard, `ServiceTask.setStatus` guard, invoice totals not settable |
| Inheritance | `Person → Customer`, `Person → Employee → Mechanic/Manager`, `Vehicle → Car/Van/Motorcycle` |
| Dynamic polymorphism | `Employee.calculateSalary()`, `Vehicle.getBaseServiceFee()` called via the supertype |
| Static polymorphism | `Invoice.applyDiscount(double)` and `Invoice.applyDiscount(String,double)` |
| Abstraction | abstract `Person`, `Employee`, `Vehicle`; interface `Billable` |

## 8. Domain exceptions (`com.autocare.exception`)

| Exception | Raised when | HTTP |
|---|---|---|
| `InsufficientStockException` | stock too low for requested quantity | 409 |
| `InvalidStateTransitionException` | illegal task or invoice status move | 409 |
| `InvalidDiscountException` | bad percentage, unknown coupon, reduction above allowed | 422 |
| `ResourceNotFoundException` | any lookup miss | 404 |
| `DuplicateResourceException` | duplicate plate or email | 409 |
| `BusinessRuleViolationException` | other rule breaches (editing a non-OPEN invoice) | 422 |

All extend a common `AutoCareException extends RuntimeException`.

## 9. Money handling

The UML signatures use `double`, and the proposal's discussion of types does too. To stay faithful while avoiding float errors:

- Keep the **public method signatures** as in the UML (`double`).
- Inside methods, convert to `BigDecimal`, compute, then `setScale(2, HALF_UP)`, and return `.doubleValue()`.
- Persist with `BigDecimal` fields or `@Column(precision=10, scale=2)` mapped to `NUMERIC(10,2)`. Either store `BigDecimal` internally with `double` accessors, or use `double` fields with a converter. Pick one approach and record it in `docs/DECISIONS.md`.
- Never compare money with `==`; tests use `isEqualByComparingTo` or `within(0.005)`.

## 10. Worked example (use as a unit-test fixture)

Car (50.00), one task with 2 × oil filter at 8.00 and 1.5 billable hours at 20.00/h, discount 10 %, tax 8 %.

| Step | Value |
|---|---|
| Task parts cost | 16.00 |
| Task labour | 30.00 |
| Task cost | 46.00 |
| Subtotal | 50.00 + 46.00 = 96.00 |
| Discount (10 %) | 9.60 |
| Taxable | 86.40 |
| Tax (8 %) | 6.91 (6.912 rounded) |
| **Total** | **93.31** |

Salary fixtures: Mechanic base 1000, overtime 10 h at 15 → 1150. Manager base 2000, bonus 300 → 2300.
