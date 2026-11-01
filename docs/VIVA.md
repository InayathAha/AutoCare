# AutoCare — Viva Defence Pack & Architectural Guide

This pack prepares the team for the viva examination. Every claim in this document is verified against the codebase.

---

## 1. Five-Minute Live Demo Script (Scenario 1)

Follow this exact workflow during the presentation / viva:

1. **Start System:**
   - Backend: `mvn spring-boot:run` (runs on `http://localhost:8080`)
   - Frontend: `npm run dev` in `frontend/` (opens `http://localhost:5173`)
   - Show the **Dashboard**: Note initial counts for Active Tasks, Low Stock, Unpaid Invoices, and Registered Customers.

2. **Register Customer and Vehicle (Dynamic Polymorphism Demo):**
   - Click tab **"Customers & Vehicles"**.
   - Click **"+ New Customer"**: Register `"John Doe"`, phone `"0771234567"`, email `"john@example.com"`.
   - Select John Doe from list, click **"+ Register Vehicle"**.
   - Choose **Type: CAR**, Plate: `"WP-CAB-1234"`, Make: `"Toyota"`, Model: `"Corolla"`, Seating: `5`.
   - Point out: `baseServiceFee` is automatically computed as **$50.00** via `Car.getBaseServiceFee()`. If a **VAN** was chosen it would be **$85.00**; for a **MOTORCYCLE** it would be **$30.00**.

3. **Open Invoice (Billable Realization):**
   - Click tab **"Invoices & Billing"**.
   - Click **"+ Open New Invoice"**, select `"John Doe"` and vehicle `"WP-CAB-1234"`.
   - Show Invoice Detail: Base fee is $50.00, status is `OPEN`, subtotal is $50.00.

4. **Assign Task to Mechanic & Record Labour:**
   - Click **"+ Manage Tasks"** (or switch to **"Task Board"** tab).
   - Click **"+ New Task"**: Description `"Standard 5,000km Engine Maintenance"`, select mechanic `"Bob Mechanic"`.
   - Add used part: Select `"OIL-5W30"`, Quantity `2` (Unit price $15.00 $\rightarrow$ Parts cost $30.00).
   - Log labor: Enter `1.5` hours (Mechanic hourly rate $20.00 $\rightarrow$ Labor fee $30.00).
   - Show total task cost: $\$30.00 + \$30.00 = \$60.00$.

5. **State Pattern & Lifecycle Progression:**
   - Click **"Start Task"**: status moves `PENDING` $\rightarrow$ `IN_PROGRESS`.
   - Click **"Complete Task"**: status moves `IN_PROGRESS` $\rightarrow$ `COMPLETED` (guarded by assigned mechanic).

6. **Static Polymorphism (Discount Overloads) & Tax:**
   - Return to **"Invoices & Billing"**.
   - Show that invoice subtotal is now $\$50.00 + \$60.00 = \$110.00$.
   - Demonstrate Static Polymorphism:
     - Method 1: Click **"% Apply Discount"** (calls `Invoice.applyDiscount(double percentage)`).
     - Method 2: Click **"🏷️ Apply Coupon"** (calls `Invoice.applyDiscount(String couponCode, double fixedReduction)`).
   - Click **"Issue Invoice"**: locks totals, transitions `OPEN` $\rightarrow$ `ISSUED`.
   - Tax at 8% is computed: $\$8.80$. Grand Total: $\$118.80$.

7. **Settlement, Loyalty Points, and Stock Depletion:**
   - Click **"💳 Settle Payment"**: status moves `ISSUED` $\rightarrow$ `PAID`.
   - Click **"🧾 View / Print Receipt"**: shows printable monospace receipt generated via `Billable.generateReceipt()`.
   - Return to **"Customers & Vehicles"**: John Doe now has **11 loyalty points** ($\lfloor 118.80 \times 0.1 \rfloor$).
   - Return to **"Inventory"**: `"OIL-5W30"` stock is reduced by 2.

---

## 2. Core OOP Principles Mapping Matrix

| OOP Concept | Evidence in AutoCare Codebase | File Location | Key Method / Annotation |
|---|---|---|---|
| **Encapsulation** | Strict private fields, zero public setters on business state, domain invariant guards. | `domain/InventoryItem.java`<br>`domain/ServiceTask.java`<br>`domain/Invoice.java` | `deductStock(int)`<br>`startTask()`, `completeTask()`<br>`calculateSubtotal()`, `calculateTotal()` |
| **Inheritance** | Single-table inheritance hierarchy for vehicles; mapped superclasses for personnel. | `domain/Vehicle.java`<br>`domain/Car.java`<br>`domain/Person.java`<br>`domain/Employee.java` | `@Inheritance(SINGLE_TABLE)`<br>`Car extends Vehicle`<br>`Person -> Employee -> Mechanic` |
| **Dynamic Polymorphism** | Method overriding without `instanceof` or `switch` on type; runtime dispatch. | `domain/Vehicle.java`<br>`domain/Car.java`<br>`domain/Van.java`<br>`domain/Employee.java`<br>`domain/Mechanic.java` | `Vehicle.getBaseServiceFee()`<br>`Car.getBaseServiceFee()` ($50)<br>`Van.getBaseServiceFee()` ($85)<br>`Employee.calculateSalary()` |
| **Static Polymorphism** | Compile-time method overloading with distinct signatures for discount logic. | `domain/Invoice.java` | `applyDiscount(double percentage)`<br>`applyDiscount(String code, double amount)` |
| **Abstraction** | Abstract classes specifying contracts without complete implementation; pure interfaces. | `domain/Person.java`<br>`domain/Employee.java`<br>`domain/Vehicle.java`<br>`domain/Billable.java` | `abstract class Person`<br>`abstract class Employee`<br>`abstract class Vehicle`<br>`interface Billable` |
| **Interface Realization** | Realization of pure interface by persistent domain entity. | `domain/Billable.java`<br>`domain/Invoice.java` | `public class Invoice implements Billable` |
| **Composition** | Strong lifecycle ownership; lifecycle of tasks tied to the parent invoice. | `domain/Invoice.java`<br>`domain/ServiceTask.java` | `@OneToMany(cascade = ALL, orphanRemoval = true)` on `Invoice.taskList` |

---

## 3. Top 20 Viva Questions and Answers

### Q1: How does AutoCare demonstrate Dynamic Polymorphism (Method Overriding)?
**Answer:** Dynamic polymorphism is demonstrated in two key domain hierarchies:
1. **Vehicle Base Service Fee Calculation:** The abstract class `Vehicle` (`backend/src/main/java/com/autocare/domain/Vehicle.java#L102`) defines `public abstract double getBaseServiceFee()`. Each subclass provides its own implementation without type-checking:
   - `Car.getBaseServiceFee()` returns `50.00` (`domain/Car.java#L58`)
   - `Van.getBaseServiceFee()` returns `85.00` (`domain/Van.java#L58`)
   - `Motorcycle.getBaseServiceFee()` returns `30.00` (`domain/Motorcycle.java#L58`)
   In `Invoice.calculateSubtotal()` (`domain/Invoice.java#L190`), the invoice invokes `vehicle.getBaseServiceFee()` through the base `Vehicle` reference. The JVM dispatches to the correct concrete method dynamically at runtime.
2. **Employee Salary Calculation:** `Employee.calculateSalary()` (`domain/Employee.java#L54`) is abstract. `Mechanic.calculateSalary()` computes `baseSalary + (overtimeHours * hourlyRate)` (`domain/Mechanic.java#L80`), while `Manager.calculateSalary()` computes `baseSalary + monthlyBonus` (`domain/Manager.java#L57`).

### Q2: How does AutoCare demonstrate Static Polymorphism (Method Overloading)?
**Answer:** Static polymorphism is demonstrated in `Invoice` (`backend/src/main/java/com/autocare/domain/Invoice.java`) through two overloaded versions of `applyDiscount`:
1. `applyDiscount(double percentage)` (`domain/Invoice.java#L258`) applies a percentage deduction against the subtotal.
2. `applyDiscount(String couponCode, double fixedReduction)` (`domain/Invoice.java#L318`) validates a promotional voucher against the configured billing policy and subtracts a fixed dollar amount.
The compiler resolves which method to invoke based on method parameter types and arity at compile time.

### Q3: Why are there no public setters for invoice totals or task status? How is Encapsulation maintained?
**Answer:** In accordance with DDD (Domain-Driven Design) and OOP best practices, domain entities are **rich models**, not anemic data structures:
- In `Invoice.java`, fields `subtotal`, `taxAmount`, and `totalAmount` have private access and NO setters. They are derived properties computed internally by `calculateSubtotal()` and `calculateTotal()`. Exposing setters would allow invalid financial states.
- In `ServiceTask.java`, the `status` field cannot be modified arbitrarily. It is guarded by `startTask()` and `completeTask()`, enforcing the state transition rule `PENDING -> IN_PROGRESS -> COMPLETED`. Any illegal transition throws `InvalidStateTransitionException`.
- In `InventoryItem.java`, `stockQuantity` cannot be set directly; it is modified solely through `deductStock(quantity)` and `restock(quantity)`.

### Q4: Why are `Person`, `Employee`, and `Vehicle` abstract classes rather than concrete classes or interfaces?
**Answer:**
- They represent generalized concepts that cannot exist on their own in the business domain. You cannot register a generic "Vehicle" without knowing if it is a Car, Van, or Motorcycle; similarly, you cannot hire an "Employee" without knowing their job role (Mechanic or Manager).
- Abstract classes allow sharing state (such as `name`, `phone`, `email` in `Person`, or `licensePlate`, `brand`, `model` in `Vehicle`) and concrete method logic (`getContactDetails()`), while forcing subclasses to provide type-specific behaviors via abstract methods (`displayProfile()`, `calculateSalary()`, `getBaseServiceFee()`).

### Q5: What is the purpose of the `Billable` interface? Why not just define those methods directly on `Invoice`?
**Answer:** `Billable` (`backend/src/main/java/com/autocare/domain/Billable.java`) defines a contract for anything that can produce a final charge and itemized receipt:
```java
public interface Billable {
    double calculateTotal();
    String generateReceipt();
}
```
This adheres to the **Interface Segregation Principle** and **Dependency Inversion Principle**. In the future, the garage could introduce other billable items (e.g. `DirectPartSale`, `StorageFeeTicket`) that implement `Billable` and can be processed by a generic billing processor `List<Billable>` without modifying existing invoice logic.

### Q6: How does AutoCare enforce the Open/Closed Principle (OCP)?
**Answer:** The codebase is open for extension but closed for modification. If the garage adds a new vehicle type, such as `ElectricCar` or `HeavyTruck`:
1. We create a new subclass `Truck extends Vehicle`.
2. We implement `getBaseServiceFee()` (e.g. $120.00) and `getVehicleType()`.
3. We add the discriminator value to `VehicleFactory`.
No existing billing calculations in `Invoice`, controllers, or repositories need to be changed or touched with `if/else` checks.

### Q7: How are business invariants enforced when service tasks transition status?
**Answer:** The status transitions are modeled as a strict finite-state machine in `ServiceTask.java`:
- Allowed: `PENDING -> IN_PROGRESS` (via `startTask()`, line 204)
- Allowed: `IN_PROGRESS -> COMPLETED` (via `completeTask()`, line 219)
- Forbidden: `PENDING -> COMPLETED` (skipping in-progress), `COMPLETED -> PENDING` (backward moves), or any re-triggering.
Any invalid move throws `InvalidStateTransitionException`, which the REST layer maps to HTTP 409 Conflict. Once `COMPLETED`, the task is locked against any further mutations.

### Q8: How does the system handle concurrent stock deduction without race conditions?
**Answer:** In `InventoryItemRepository.java`:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM InventoryItem i WHERE i.partCode = :partCode")
Optional<InventoryItem> findByPartCodeForUpdate(@Param("partCode") String partCode);
```
`TaskService.addPart()` acquires a pessimistic write lock (`SELECT ... FOR UPDATE` in PostgreSQL). When two mechanics simultaneously attempt to use the last unit of a spare part, transaction 1 locks the row, validates stock, decrements to 0, and commits. Transaction 2 unblocks, reads the updated stock (0), fails the domain check `item.deductStock(quantity)`, and throws `InsufficientStockException` (HTTP 409).

### Q9: How is Manager Authorization enforced for discounts greater than the policy limit?
**Answer:** In `Invoice.java` (`applyDiscount(double percentage, BillingPolicy policy, String managerId)`):
- If `percentage > policy.maxUnapprovedDiscountPct()` (15%) and `managerId` is null or blank, the domain model throws `InvalidDiscountException("... manager approval required")`.
- In `InvoiceService.java`, if a `managerId` is supplied, it verifies that the employee exists and is an instance of `Manager`, then calls `manager.approveDiscount(invoice, percentage)`.
- Without a valid manager, the REST endpoint returns HTTP 422 Unprocessable Entity with code `INVALID_DISCOUNT`.

### Q10: How are database entities mapped to inheritance hierarchies in PostgreSQL?
**Answer:** Both `Vehicle` and `Employee` use JPA's `InheritanceType.SINGLE_TABLE` strategy:
- `vehicles` table: has a `vehicle_type` discriminator column (`'CAR'`, `'VAN'`, `'MOTORCYCLE'`) and sparse subclass columns (`seating_capacity`, `cargo_volume`, `engine_cc`).
- `employees` table: has an `employee_role` discriminator column (`'MECHANIC'`, `'MANAGER'`) and role-specific columns (`specialization`, `overtime_hours`, `hourly_rate`, `monthly_bonus`).
Database integrity is guaranteed by SQL CHECK constraints defined in `V1__init_schema.sql`:
`CONSTRAINT vehicles_type_columns CHECK (...)` and `CONSTRAINT employees_role_columns CHECK (...)`.

### Q11: Why are Domain Entities never returned directly from REST controllers?
**Answer:** To strictly uphold separation of concerns and avoid entity leakage:
1. **Security & Data Integrity:** Prevents internal database details, password hashes, or unmanaged JPA proxies from leaking into JSON.
2. **Circular Reference Prevention:** JPA bidirectional relationships (`Invoice -> ServiceTask -> Invoice`) cause infinite loops during Jackson serialization if entities are exposed directly.
3. **Contract Stability:** API contracts (`com.autocare.dto.*`) remain stable even if database schema columns or internal entities change.

### Q12: How are DTOs synchronized between backend and frontend?
**Answer:** Backend DTOs are Java 21 `record` types located in `backend/src/main/java/com/autocare/dto/`. The frontend mirrors them field-for-field as TypeScript interfaces in `frontend/src/types/index.ts`. All numeric values representing money use `number` in TypeScript and `double` in DTO records, and dates are serialized as ISO-8601 UTC strings.

### Q13: How does AutoCare handle money arithmetic without floating-point inaccuracies?
**Answer:** While public UML method signatures specify `double` for module requirements, all internal math inside domain entities (`Invoice`, `ServiceTask`, `InventoryItem`) converts amounts to `BigDecimal` with `RoundingMode.HALF_UP` and scale 2:
```java
BigDecimal sub = BigDecimal.valueOf(calculateSubtotal()).setScale(2, RoundingMode.HALF_UP);
BigDecimal pct = BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
this.discountAmount = sub.multiply(pct).setScale(2, RoundingMode.HALF_UP);
```
This eliminates binary floating-point representation drift (e.g. `0.1 + 0.2 = 0.30000000000000004`).

### Q14: How does the system prevent deleting a customer who has registered vehicles or active invoices?
**Answer:** Multi-layer protection:
1. **Database Level:** In `V1__init_schema.sql`, `vehicles.customer_id` and `invoices.customer_id` specify `ON DELETE RESTRICT`.
2. **Application Level:** In `CustomerService.java#delete()`, the service validates that the customer has no registered vehicles or invoices before deleting.
3. Attempting to delete returns HTTP 409 Conflict with code `FK_CONSTRAINT`.

### Q15: What is snapshot pricing and why is it implemented on `TaskUsedPart`?
**Answer:** When an inventory item is added to a task, `TaskUsedPart` captures the item's current `unitPrice` at that instant. If the garage subsequently increases the price of "Engine Oil" in the inventory table, existing invoices and tasks preserve the historical snapshot price they were billed at, guaranteeing financial audit integrity.

### Q16: How is the Factory Pattern implemented in AutoCare?
**Answer:** In `VehicleFactory.java` and `EmployeeFactory.java`:
- `VehicleFactory.createVehicle(VehicleRequest, Customer)` inspects the polymorphic `request.type()` discriminator and instantiates the proper subclass (`Car`, `Van`, or `Motorcycle`) while validating type-specific fields (e.g. positive cargo volume for Van).
- Services depend solely on `VehicleFactory` rather than hardcoding subclass instantiation.

### Q17: What is the relationship between `Invoice` and `ServiceTask`?
**Answer:** It is a **Composition** relationship (represented by a filled diamond in UML). A `ServiceTask` cannot exist without an `Invoice`. In `Invoice.java`:
```java
@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ServiceTask> taskList = new ArrayList<>();
```
Deleting an unissued invoice deletes all of its itemized tasks.

### Q18: How does loyalty point redemption and accrual work?
**Answer:**
- **Accrual:** Upon calling `Invoice.settle()`, the customer earns 1 loyalty point for every full $10 spent ($\lfloor \text{totalAmount} / 10 \rfloor$) via `customer.addLoyaltyPoints()`.
- **Redemption:** Customers can redeem points through `Customer.redeemPoints(points)`. Points must be positive and cannot exceed current balance; otherwise `IllegalArgumentException` is thrown.

### Q19: What exception handling architecture is used?
**Answer:** All custom business exceptions extend `AutoCareException extends RuntimeException` (`domain/exception`). A single `@RestControllerAdvice` (`GlobalExceptionHandler.java`) intercepts all domain and database exceptions and maps them to standard RFC-compliant `ApiError` payloads containing `timestamp`, `status`, `error`, `code`, `message`, `path`, and optional `fieldErrors`.

### Q20: What automated testing strategies verify the system?
**Answer:**
1. **Domain Unit Tests:** Pure JUnit 5 tests running without Spring or database to verify domain invariants in isolation (`InvoiceTest`, `ServiceTaskTest`, `InventoryItemTest`, `VehicleTest`, `CustomerTest`).
2. **Repository Integration Tests:** Testcontainers running PostgreSQL with Flyway migration to test SQL constraints and polymorphic queries (`RepositoryIntegrationTest`).
3. **Service Integration Tests:** Verification of transactional rollbacks (`TaskServiceIntegrationTest`).
4. **End-to-End Scenarios:** Full workflow tests exercising HTTP endpoints through MockMvc across all 5 user scenarios (`EndToEndScenariosTest`).
