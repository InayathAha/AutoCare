-- V2__seed_dev_data.sql
-- Development seed data for AutoCare Garage Management System
-- Only executed under 'dev' profile

-- 1. Customers
INSERT INTO customers (customer_id, name, phone, email, loyalty_points, created_at)
VALUES
    ('c0000001-0000-0000-0000-000000000001', 'Alice Smith',    '0771122334', 'alice.smith@example.com',    120, now()),
    ('c0000001-0000-0000-0000-000000000002', 'Bob Johnson',    '0772233445', 'bob.johnson@example.com',     45, now()),
    ('c0000001-0000-0000-0000-000000000003', 'Charlie Brown',  '0773344556', 'charlie.brown@example.com',    0, now())
ON CONFLICT (customer_id) DO NOTHING;

-- 2. Employees (2 Mechanics with different specializations, 1 Manager)
INSERT INTO employees (employee_id, name, phone, email, base_salary, employee_role, specialization, overtime_hours, hourly_rate, monthly_bonus)
VALUES
    ('e0000001-0000-0000-0000-000000000001', 'Dave Miller',   '0774455667', 'dave.miller@garage.com',   2200.00, 'MECHANIC', 'Engine',              5, 30.00, NULL),
    ('e0000001-0000-0000-0000-000000000002', 'Edward White',  '0775566778', 'edward.white@garage.com',  2100.00, 'MECHANIC', 'Brakes & Suspension', 0, 28.00, NULL),
    ('e0000001-0000-0000-0000-000000000003', 'Fiona Clark',   '0776677889', 'fiona.clark@garage.com',   3500.00, 'MANAGER',  NULL,               NULL,  NULL, 500.00)
ON CONFLICT (employee_id) DO NOTHING;

-- 3. Vehicles (2 of each type: Car, Van, Motorcycle)
INSERT INTO vehicles (license_plate, customer_id, vehicle_type, brand, model, base_service_fee, seating_capacity, cargo_volume, engine_cc, registered_at)
VALUES
    ('CAB-1001', 'c0000001-0000-0000-0000-000000000001', 'CAR',        'Toyota',    'Camry',         50.00, 5,    NULL, NULL, now()),
    ('CAB-1002', 'c0000001-0000-0000-0000-000000000002', 'CAR',        'Honda',     'Civic',         50.00, 5,    NULL, NULL, now()),
    ('VAN-2001', 'c0000001-0000-0000-0000-000000000001', 'VAN',        'Ford',      'Transit',       85.00, NULL, 15.20, NULL, now()),
    ('VAN-2002', 'c0000001-0000-0000-0000-000000000003', 'VAN',        'Mercedes',  'Sprinter',      85.00, NULL, 17.50, NULL, now()),
    ('MOT-3001', 'c0000001-0000-0000-0000-000000000002', 'MOTORCYCLE', 'Yamaha',    'MT-07',         30.00, NULL, NULL,  689, now()),
    ('MOT-3002', 'c0000001-0000-0000-0000-000000000003', 'MOTORCYCLE', 'Kawasaki',  'Ninja 650',     30.00, NULL, NULL,  649, now())
ON CONFLICT (license_plate) DO NOTHING;

-- 4. Inventory Items (8 items, exactly 2 with stock <= threshold)
INSERT INTO inventory_items (part_code, part_name, unit_price, stock_quantity, reorder_threshold, last_restocked_at)
VALUES
    ('OIL-5W30',  'Fully Synthetic 5W-30 Engine Oil (1L)', 35.00,  40, 10, now()),
    ('FLT-OIL01', 'Premium Spin-on Oil Filter',            15.00,  25,  8, now()),
    ('BRK-PAD01', 'Front Ceramic Brake Pads Set',          65.00,   3,  5, now()), -- LOW STOCK (3 <= 5)
    ('BRK-ROT01', 'Vented Front Brake Rotor',              95.00,   2,  4, now()), -- LOW STOCK (2 <= 4)
    ('SPK-IRID',  'Laser Iridium Spark Plug',              12.50,  50, 12, now()),
    ('BAT-12V70', '12V 70Ah AGM Automotive Battery',      140.00,   8,  3, now()),
    ('BLT-TIM01', 'High Performance Timing Belt Kit',     110.00,   6,  2, now()),
    ('WPR-24IN',  'All-Weather Wiper Blade 24-inch',       18.00,  15,  5, now())
ON CONFLICT (part_code) DO NOTHING;

-- 5. Invoice with Completed Service Task and Used Parts
INSERT INTO invoices (invoice_id, customer_id, license_plate, subtotal, discount_amount, tax_rate, total_amount, payment_status, issued_at, settled_at)
VALUES
    ('i0000001-0000-0000-0000-000000000001',
     'c0000001-0000-0000-0000-000000000001',
     'CAB-1001',
     175.00,
     0.00,
     8.00,
     189.00,
     'ISSUED',
     now(),
     NULL)
ON CONFLICT (invoice_id) DO NOTHING;

INSERT INTO service_tasks (task_id, invoice_id, assigned_mechanic_id, description, status, billable_hours, labor_charge, parts_cost, total_task_cost, created_at)
VALUES
    ('t0000001-0000-0000-0000-000000000001',
     'i0000001-0000-0000-0000-000000000001',
     'e0000001-0000-0000-0000-000000000001',
     'Routine 10k service: Engine oil change & oil filter replacement',
     'COMPLETED',
     1.50,
     75.00,
     50.00,
     125.00,
     now())
ON CONFLICT (task_id) DO NOTHING;

INSERT INTO task_used_parts (task_id, part_code, quantity, unit_price, subtotal, attached_at)
VALUES
    ('t0000001-0000-0000-0000-000000000001', 'OIL-5W30',  1, 35.00, 35.00, now()),
    ('t0000001-0000-0000-0000-000000000001', 'FLT-OIL01', 1, 15.00, 15.00, now())
ON CONFLICT (task_id, part_code) DO NOTHING;
