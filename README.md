# AutoCare — Garage & Service Management System

AutoCare digitises vehicle garage workflows: customer and vehicle registration, mechanic job dispatching, spare-parts inventory tracking with low-stock alerts, diagnostics, and itemised billing with discounts and taxes.

Built for the **Object-Oriented Programming (OOP)** module to showcase core OOP principles: **Encapsulation**, **Inheritance**, **Dynamic & Static Polymorphism**, **Abstraction**, and **Interface Realization**.

---

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.3.5, Maven, Spring Data JPA (Hibernate), Flyway
- **Database:** Supabase (Managed PostgreSQL 17) via JDBC only (No Supabase client SDK, No PostgREST)
- **Frontend:** React, TypeScript, Vite, Tailwind CSS
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

### Backend
```bash
cd backend
mvn clean test
mvn spring-boot:run
```

To run with dev seed data:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
