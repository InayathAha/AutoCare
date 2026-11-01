# 03 — Database Schema (Supabase PostgreSQL)

Source: the ERD (single-table inheritance for vehicles and employees, a junction table for parts consumption, a historical price snapshot, `ON DELETE RESTRICT` everywhere).
Owner: Persistence layer (Bhakeer). Engine: PostgreSQL on Supabase, accessed **only** through Spring Data JPA / JDBC.

## 1. Entities and relationships

| Relationship | Cardinality | FK |
|---|---|---|
| customers → vehicles | 1 : 0..* | `vehicles.customer_id` |
| customers → invoices | 1 : 0..* | `invoices.customer_id` |
| vehicles → invoices | 1 : 0..* | `invoices.license_plate` |
| invoices → service_tasks | 1 : 1..* | `service_tasks.invoice_id` |
| employees → service_tasks | 1 : 0..* | `service_tasks.assigned_mechanic_id` (must reference a MECHANIC) |
| service_tasks ↔ inventory_items | M : N | via `task_used_parts` |

Design notes carried over from the ERD:

- **Single-table inheritance** for `vehicles` (discriminator `vehicle_type`) and `employees` (discriminator `employee_role`) to avoid excess joins. Subclass-only columns are nullable.
- **Historical price snapshot:** `task_used_parts.unit_price` records the selling price at attach time, so later price changes never alter old invoices.
- **Referential integrity:** all FKs use `ON DELETE RESTRICT`.
- **ID strategy:** `VARCHAR(36)` UUID strings, with natural keys for vehicles (plate) and inventory (part code).

## 2. Flyway migration `V1__init_schema.sql`

```sql
-- Customers
CREATE TABLE customers (
    customer_id    VARCHAR(36)  PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    loyalty_points INTEGER      NOT NULL DEFAULT 0 CHECK (loyalty_points >= 0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ
);

-- Vehicles (single-table inheritance)
CREATE TABLE vehicles (
    license_plate    VARCHAR(20)   PRIMARY KEY,
    customer_id      VARCHAR(36)   NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT,
    brand            VARCHAR(50)   NOT NULL,
    model            VARCHAR(50)   NOT NULL,
    vehicle_type     VARCHAR(20)   NOT NULL CHECK (vehicle_type IN ('CAR','VAN','MOTORCYCLE')),
    base_service_fee NUMERIC(10,2) NOT NULL CHECK (base_service_fee >= 0),
    seating_capacity INTEGER       CHECK (seating_capacity > 0),   -- Car only
    cargo_volume     NUMERIC(6,2)  CHECK (cargo_volume > 0),       -- Van only
    engine_cc        INTEGER       CHECK (engine_cc > 0),          -- Motorcycle only
    registered_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT vehicles_type_columns CHECK (
        (vehicle_type = 'CAR'        AND seating_capacity IS NOT NULL) OR
        (vehicle_type = 'VAN'        AND cargo_volume     IS NOT NULL) OR
        (vehicle_type = 'MOTORCYCLE' AND engine_cc        IS NOT NULL)
    )
);
CREATE INDEX idx_vehicles_customer ON vehicles(customer_id);

-- Employees (single-table inheritance)
CREATE TABLE employees (
    employee_id    VARCHAR(36)   PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    phone          VARCHAR(20)   NOT NULL,
    email          VARCHAR(100)  NOT NULL UNIQUE,
    base_salary    NUMERIC(10,2) NOT NULL CHECK (base_salary >= 0),
    employee_role  VARCHAR(30)   NOT NULL CHECK (employee_role IN ('MECHANIC','MANAGER')),
    specialization VARCHAR(50),                                    -- Mechanic only
    overtime_hours INTEGER       CHECK (overtime_hours >= 0),      -- Mechanic only
    hourly_rate    NUMERIC(10,2) CHECK (hourly_rate >= 0),         -- Mechanic only
    monthly_bonus  NUMERIC(10,2) CHECK (monthly_bonus >= 0),       -- Manager only
    hired_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT employees_role_columns CHECK (
        (employee_role = 'MECHANIC' AND overtime_hours IS NOT NULL AND hourly_rate IS NOT NULL) OR
        (employee_role = 'MANAGER'  AND monthly_bonus  IS NOT NULL)
    )
);

-- Inventory
CREATE TABLE inventory_items (
    part_code         VARCHAR(50)   PRIMARY KEY,
    part_name         VARCHAR(100)  NOT NULL,
    unit_price        NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    stock_quantity    INTEGER       NOT NULL CHECK (stock_quantity >= 0),
    reorder_threshold INTEGER       NOT NULL CHECK (reorder_threshold >= 0),
    last_restocked_at TIMESTAMPTZ
);

-- Invoices
CREATE TABLE invoices (
    invoice_id      VARCHAR(36)   PRIMARY KEY,
    customer_id     VARCHAR(36)   NOT NULL REFERENCES customers(customer_id) ON DELETE RESTRICT,
    license_plate   VARCHAR(20)   NOT NULL REFERENCES vehicles(license_plate) ON DELETE RESTRICT,
    subtotal        NUMERIC(10,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    tax_rate        NUMERIC(5,2)  NOT NULL DEFAULT 0 CHECK (tax_rate >= 0),
    total_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    payment_status  VARCHAR(20)   NOT NULL DEFAULT 'OPEN' CHECK (payment_status IN ('OPEN','ISSUED','PAID')),
    issued_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    settled_at      TIMESTAMPTZ
);
CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_vehicle  ON invoices(license_plate);

-- Service tasks (job cards)
CREATE TABLE service_tasks (
    task_id               VARCHAR(36)   PRIMARY KEY,
    invoice_id            VARCHAR(36)   NOT NULL REFERENCES invoices(invoice_id) ON DELETE RESTRICT,
    assigned_mechanic_id  VARCHAR(36)   NOT NULL REFERENCES employees(employee_id) ON DELETE RESTRICT,
    description           TEXT          NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED')),
    billable_hours        NUMERIC(4,2)  NOT NULL DEFAULT 0 CHECK (billable_hours >= 0),
    labor_charge          NUMERIC(10,2) NOT NULL DEFAULT 0,
    parts_cost            NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_task_cost       NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_tasks_invoice  ON service_tasks(invoice_id);
CREATE INDEX idx_tasks_mechanic ON service_tasks(assigned_mechanic_id);
CREATE INDEX idx_tasks_status   ON service_tasks(status);

-- Junction: parts consumed per task
CREATE TABLE task_used_parts (
    used_part_id BIGSERIAL     PRIMARY KEY,
    task_id      VARCHAR(36)   NOT NULL REFERENCES service_tasks(task_id)    ON DELETE RESTRICT,
    part_code    VARCHAR(50)   NOT NULL REFERENCES inventory_items(part_code) ON DELETE RESTRICT,
    quantity     INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),   -- snapshot
    subtotal     NUMERIC(10,2) NOT NULL,
    attached_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_task_part UNIQUE (task_id, part_code)
);
CREATE INDEX idx_used_parts_part ON task_used_parts(part_code);

-- Lock the tables away from Supabase's auto-generated REST API.
-- The Java backend connects as the table owner (bypasses RLS); anon/authenticated roles get nothing.
ALTER TABLE customers        ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles         ENABLE ROW LEVEL SECURITY;
ALTER TABLE employees        ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_items  ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices         ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_tasks    ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_used_parts  ENABLE ROW LEVEL SECURITY;
```

Two small additions beyond the ERD, both to be logged in `DECISIONS.md`:

1. `UNIQUE (task_id, part_code)` on `task_used_parts`, so a part appears once per task, which matches `Map<InventoryItem,Integer>` in the UML. Adding the same part again merges quantity.
2. Type-specific `CHECK` constraints so a CAR row cannot lack `seating_capacity`, and so on.

Also note the `invoices.payment_status` column carries the three-step lifecycle (`OPEN → ISSUED → PAID`) described in `01-requirements.md`. The ERD lists `payment_status` but not its values.

## 3. Supabase connection

Use a Supabase **connection string for the Postgres database** (Project Settings → Database). Prefer the session pooler on port 5432 for a long-running Spring app. If you use the transaction pooler (port 6543), add `prepareThreshold=0` to the JDBC URL.

`application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}              # jdbc:postgresql://<host>:5432/postgres?sslmode=require
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 5      # Supabase free tier has limited connections
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
```

`.env.example` (commit this, never the real `.env`):

```
DB_URL=jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.PROJECT_REF
DB_PASSWORD=change-me
```

Local dev and CI tests should use Testcontainers PostgreSQL rather than the shared cloud database.

## 4. JPA mapping cheat sheet

| Class | Mapping |
|---|---|
| `Person` | `@MappedSuperclass`; fields `id`, `name`, `phone`, `email` |
| `Customer` | `@Entity @Table("customers")`, `@AttributeOverride(name="id", column=@Column(name="customer_id", length=36))` |
| `Employee` | `@Entity @Table("employees") @Inheritance(SINGLE_TABLE) @DiscriminatorColumn(name="employee_role")`, id override → `employee_id` |
| `Mechanic` / `Manager` | `@DiscriminatorValue("MECHANIC")` / `("MANAGER")` |
| `Vehicle` | `@Entity @Table("vehicles") @Inheritance(SINGLE_TABLE) @DiscriminatorColumn(name="vehicle_type")`, `@Id` on `licensePlate` → `license_plate` |
| `Car` / `Van` / `Motorcycle` | `@DiscriminatorValue("CAR"/"VAN"/"MOTORCYCLE")` |
| `InventoryItem` | `@Id partCode`; `@Version` optional but recommended to catch concurrent stock updates |
| `ServiceTask` | `@ManyToOne(fetch=LAZY) Invoice`, `@ManyToOne(fetch=LAZY) Mechanic`, `@OneToMany(mappedBy="task", cascade=ALL, orphanRemoval=true) List<TaskUsedPart>` |
| `Invoice` | `@OneToMany(mappedBy="invoice", cascade=ALL, orphanRemoval=true) List<ServiceTask>` |

Field access (`@Id` on fields) keeps JPA from forcing public setters. Hibernate needs a no-arg constructor; make it `protected`.

Mapping gotchas:

- The discriminator column must not also be mapped as a normal field. Expose it through `getVehicleType()` and `getEmployeeRole()` returning constants.
- `Mechanic` and `Manager` columns are nullable at the table level; the entity fields use wrapper-safe defaults.
- `Invoice.status` maps to `payment_status`; keep the enum `InvoiceStatus {OPEN, ISSUED, PAID}`.
- Disable `open-in-view`, fetch lazily, and use `@EntityGraph` or fetch joins for invoice detail reads.

## 5. Repositories (`com.autocare.repository`)

```java
interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByEmail(String email);
}
interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByOwnerId(String customerId);
}
interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Mechanic> findMechanicsBySpecialization(String specialization); // use a typed query on Mechanic
}
interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    @Query("select i from InventoryItem i where i.stockQuantity <= i.reorderThreshold")
    List<InventoryItem> findLowStock();
}
interface ServiceTaskRepository extends JpaRepository<ServiceTask, String> {
    List<ServiceTask> findByStatus(TaskStatus status);
    List<ServiceTask> findByAssignedMechanicEmployeeId(String mechanicId);
}
interface InvoiceRepository extends JpaRepository<Invoice, String> {
    List<Invoice> findByCustomerIdOrderByIssuedAtDesc(String customerId);
}
```

Adjust derived-query property paths to the final field names. Repositories contain no business logic.

## 6. Seed data (`V2__seed_dev_data.sql`, dev profile only)

Provide at least: 3 customers, 2 vehicles of each type, 2 mechanics (different specialisations), 1 manager, 8 inventory items with 2 below threshold, and 1 invoice with a completed task and used parts. Use this for demos and for frontend work before the full flow exists. Keep it out of the production profile (`spring.flyway.locations` per profile).

## 7. Persistence-layer acceptance checks

- Fresh Supabase (or Testcontainers) DB migrates cleanly with Flyway.
- App boots with `ddl-auto=validate`.
- Saving a `Car` writes `vehicle_type='CAR'`, `base_service_fee=50.00`; loading returns a `Car` instance (not a bare `Vehicle`).
- Deleting a customer with vehicles fails with an FK violation, translated to HTTP 409.
- Attaching a part snapshots `unit_price`; changing `inventory_items.unit_price` afterwards does not alter the stored snapshot.
- Setting `stock_quantity` negative directly in SQL is rejected by the CHECK constraint.
