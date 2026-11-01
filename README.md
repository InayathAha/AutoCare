# AutoCare — Garage & Service Management System

AutoCare digitises vehicle garage workflows: customer and vehicle registration, mechanic job dispatching, spare-parts inventory tracking with low-stock alerts, diagnostics, and itemised billing with discounts and taxes.

Built for the **Object-Oriented Programming (OOP)** module to showcase core OOP principles: **Encapsulation**, **Inheritance**, **Dynamic & Static Polymorphism**, **Abstraction**, and **Interface Realization**.

---

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.3.5, Maven, Spring Data JPA (Hibernate), Flyway
- **Database:** Supabase (Managed PostgreSQL 17) via JDBC only (No Supabase client SDK, No PostgREST)
- **Frontend:** React 19, TypeScript (strict mode), Vite, Modern Responsive CSS
- **Testing:** JUnit 5, AssertJ, Mockito, Testcontainers PostgreSQL

---

## Database Connection & Supabase Pooler Configuration

All database access from AutoCare goes through the Java backend using PostgreSQL JDBC.

### Environment Variables

Copy `.env.example` to `.env` in the root directory (never commit `.env`):

```bash
DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<your-db-password>
```

### Connection Pooler Guidance

1. **Session Pooler (Port 5432 - Recommended):**
   - URL: `jdbc:postgresql://<db-host>:5432/postgres?sslmode=require`
   - Keeps persistent backend sessions. Recommended for standard Spring Boot operation with HikariCP.
   
2. **Transaction Pooler (Port 6543):**
   - URL: `jdbc:postgresql://<db-host>:6543/postgres?sslmode=require&prepareThreshold=0`
   - If using port 6543, PostgreSQL server-side prepared statements must be disabled by appending `prepareThreshold=0` to prevent statement cache conflicts.

### Database Hardening & Row Level Security (RLS)

AutoCare enables PostgreSQL Row Level Security (RLS) on all 7 application tables:
- `customers`
- `vehicles`
- `employees`
- `inventory_items`
- `invoices`
- `service_tasks`
- `task_used_parts`

Because no public policies (`anon` or `authenticated`) are defined, direct HTTP calls to Supabase PostgREST endpoints return zero records. All operations must proceed securely through the authenticated Spring Boot backend connection.

---

## Profiles & Flyway Migrations

- **Default Profile (`default` / `prod`):**
  Applies `V1__init_schema.sql` under `classpath:db/migration`.
- **Dev Profile (`dev`):**
  Applies both `V1__init_schema.sql` and `V2__seed_dev_data.sql` under `classpath:db/migration,classpath:db/dev-seed`.
  Provides initial demo data: 3 customers, 6 vehicles (2 Cars, 2 Vans, 2 Motorcycles), 3 employees (2 mechanics, 1 manager), 8 parts (2 low-stock), and 1 completed service task on an issued invoice.

---

## Running the Application

### 1. Backend (Spring Boot 3 + Java 21)
```bash
cd backend

# Run all automated tests
mvn clean test

# Run backend with production schema
mvn spring-boot:run

# Or run backend with dev seed data (recommended for demo)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
The backend REST API will be available at `http://localhost:8080/api/v1`.

### 2. Frontend (React + TypeScript + Vite)
```bash
cd frontend

# Install dependencies
npm install

# Run Vite development server
npm run dev

# Run TypeScript compilation and production build
npm run build

# Run code linter
npm run lint
```
The frontend UI will be available at `http://localhost:5173`.

---

## Core OOP Principles in AutoCare

The AutoCare domain model strictly demonstrates core Object-Oriented Programming concepts without anemic domain anti-patterns or artificial switch statements:

| OOP Concept | Domain Class & Location | Key Method / Invariant Guard | Explanation |
|---|---|---|---|
| **Abstraction** | `Person` ([`Person.java`](backend/src/main/java/com/autocare/domain/Person.java))<br>`Employee` ([`Employee.java`](backend/src/main/java/com/autocare/domain/Employee.java))<br>`Vehicle` ([`Vehicle.java`](backend/src/main/java/com/autocare/domain/Vehicle.java)) | `displayProfile()`<br>`calculateSalary()`<br>`getBaseServiceFee()` | Abstract base classes encapsulate shared state (ID, contact, base salary) while enforcing template methods and abstract contracts on subclasses. Cannot be instantiated directly. |
| **Inheritance** | `Customer`, `Employee` $\rightarrow$ `Person`<br>`Mechanic`, `Manager` $\rightarrow$ `Employee`<br>`Car`, `Van`, `Motorcycle` $\rightarrow$ `Vehicle` | `super(...)`, protected constructors | Hierarchical taxonomy reusing attributes and behavior, maintaining single-responsibility and clean type boundaries. |
| **Dynamic Polymorphism** | `Mechanic`, `Manager`<br>`Car`, `Van`, `Motorcycle` | `calculateSalary()`<br>`getBaseServiceFee()` | Subclasses override abstract parent methods at runtime. `Invoice` evaluates `vehicle.getBaseServiceFee()` ($50 Car / $85 Van / $30 Moto) and payroll evaluates `employee.calculateSalary()` polymorphically without `instanceof` or `switch`. |
| **Static Polymorphism** | `Invoice` ([`Invoice.java`](backend/src/main/java/com/autocare/domain/Invoice.java)) | `applyDiscount(double)`<br>`applyDiscount(String, double)` | Compile-time method overloading providing two distinct discount behaviors: percentage discount with policy caps vs coupon-based fixed reductions. |
| **Interface Realization** | `Billable` ([`Billable.java`](backend/src/main/java/com/autocare/domain/Billable.java)) $\rightarrow$ `Invoice` | `calculateTotal()`<br>`generateReceipt()` | `Invoice` implements the pure `Billable` contract, allowing polymorphic integration into payment gateways, reporting engines, or collections of billable items. |
| **Encapsulation & Invariants** | `InventoryItem` ([`InventoryItem.java`](backend/src/main/java/com/autocare/domain/InventoryItem.java))<br>`ServiceTask` ([`ServiceTask.java`](backend/src/main/java/com/autocare/domain/ServiceTask.java))<br>`Invoice` ([`Invoice.java`](backend/src/main/java/com/autocare/domain/Invoice.java)) | `deductStock(int)`<br>`startTask()`, `completeTask()`<br>`issue()`, `settle()` | All fields are strictly private. Zero public setters for state governed by business rules. Invariants (e.g. stock cannot go negative, state progression only forward, totals frozen on issue) enforced within domain methods. |

---

## 5-Minute Live Viva Demo Script (Scenario 1)

Follow this exact workflow during presentation and oral defence:

1. **System Launch:** Start backend (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`) and frontend (`npm run dev`). Open `http://localhost:5173`. Show Dashboard metrics (Active Tasks, Low Stock items, Unpaid Invoices).
2. **Dynamic Polymorphism (Vehicle Registration):**
   - Navigate to **"Customers & Vehicles"**. Register customer `"John Doe"`.
   - Register a vehicle under John Doe: Type **CAR** (`WP-CAB-1234`).
   - Point out that `baseServiceFee` is automatically evaluated as **$50.00** via `Car.getBaseServiceFee()`. (Note that VAN is $85.00, MOTORCYCLE is $30.00).
3. **Billable Realization & Invoicing:**
   - Navigate to **"Invoices & Billing"**. Open a new invoice for `"John Doe"` and `"WP-CAB-1234"`.
   - Show initial status `OPEN` and base subtotal of $50.00.
4. **Task Dispatch & Stock Deductions:**
   - In **"Task Board"**, create task: `"Standard 5,000km Maintenance"`, assign to mechanic `"Bob Mechanic"`.
   - Attach part: `2 × OIL-5W30` @ $15.00 = $30.00 (automatically decrements stock).
   - Log labour: `1.5` hours @ $20.00/h = $30.00. Total task cost = $60.00.
   - Advance task: `PENDING` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`.
5. **Static Polymorphism & Settlement:**
   - Open invoice: subtotal is now $\$50.00 + \$60.00 = \$110.00$.
   - Demonstrate overload choice: either apply percentage discount or apply coupon discount.
   - Click **"Issue Invoice"**: locks invoice totals, sets status `ISSUED`. Tax at 8% = $8.80. Grand total = $118.80.
   - Click **"Settle Invoice"**: transitions to `PAID`.
   - Click **"View / Print Receipt"**: renders formatted ASCII monospace receipt generated via `Billable.generateReceipt()`.
   - Inspect John Doe: earned **11 loyalty points** ($\lfloor 118.80 \times 0.1 \rfloor$).
   - Inspect Inventory: `"OIL-5W30"` stock decremented by 2.

---

## Verification & Architecture Documentation

- **Viva Defence Pack:** Full set of 20 architectural questions and code citations: [`docs/VIVA.md`](docs/VIVA.md).
- **UML Class Diagram (Mermaid):** [`docs/diagrams/uml-class-diagram.mermaid`](docs/diagrams/uml-class-diagram.mermaid).
- **Database ERD (Mermaid):** [`docs/diagrams/erd.mermaid`](docs/diagrams/erd.mermaid).
- **Architecture Decisions Log:** [`docs/DECISIONS.md`](docs/DECISIONS.md).
- **Automated Verification Suite:**
  - 5 End-to-End Scenarios: [`EndToEndScenariosTest.java`](backend/src/test/java/com/autocare/EndToEndScenariosTest.java)
  - Persistence & RLS Checks: [`RepositoryIntegrationTest.java`](backend/src/test/java/com/autocare/repository/RepositoryIntegrationTest.java)
  - Unit Tests: 28 test classes across domain, services, and controllers with 100% pass rate.
