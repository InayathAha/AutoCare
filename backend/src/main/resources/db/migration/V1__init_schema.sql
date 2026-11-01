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
