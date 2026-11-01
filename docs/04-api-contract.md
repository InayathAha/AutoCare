# 04 — API Contract (REST + DTOs + TypeScript)

Base URL: `/api/v1`. JSON, camelCase. No authentication (out of scope). CORS allows the Vite dev origin (`http://localhost:5173`).
Controllers use DTOs only. Each DTO is a Java `record` in `com.autocare.dto` and has an identically named TypeScript interface in `frontend/src/types/`. **Change both or neither.**

## 1. Conventions

- Create returns `201` with `Location` header and the created DTO.
- Read returns `200`. Delete returns `204`.
- Validation uses `jakarta.validation` annotations on request records (`@NotBlank`, `@Email`, `@Positive`, ...).
- Money in DTOs is a JSON number with two decimals.
- Timestamps are ISO-8601 UTC strings.
- Polymorphic payloads carry a `type` discriminator.

## 2. Error format

```json
{
  "timestamp": "2026-11-02T09:15:30Z",
  "status": 409,
  "error": "Conflict",
  "code": "INSUFFICIENT_STOCK",
  "message": "Only 2 units of OIL-5W30 in stock, requested 5",
  "path": "/api/v1/tasks/ab12/parts",
  "fieldErrors": [{ "field": "quantity", "message": "must be greater than 0" }]
}
```

`fieldErrors` is present only for `400` validation failures. Codes: `VALIDATION_FAILED` (400), `NOT_FOUND` (404), `DUPLICATE_RESOURCE` (409), `INSUFFICIENT_STOCK` (409), `INVALID_STATE_TRANSITION` (409), `INVALID_DISCOUNT` (422), `BUSINESS_RULE_VIOLATION` (422), `FK_CONSTRAINT` (409), `INTERNAL_ERROR` (500). A single `@RestControllerAdvice` maps domain exceptions to these.

## 3. Endpoints

### Module 1 — Customers & Vehicles

| Method | Path | Body → Response | Notes |
|---|---|---|---|
| POST | `/customers` | `CustomerRequest` → `CustomerResponse` | email unique |
| GET | `/customers` | → `CustomerResponse[]` | optional `?q=` name/email search |
| GET | `/customers/{id}` | → `CustomerResponse` | |
| PUT | `/customers/{id}` | `CustomerRequest` → `CustomerResponse` | |
| DELETE | `/customers/{id}` | → 204 | 409 if vehicles or invoices exist |
| POST | `/customers/{id}/vehicles` | `VehicleRequest` → `VehicleResponse` | calls `Customer.registerVehicle` |
| GET | `/customers/{id}/vehicles` | → `VehicleResponse[]` | |
| GET | `/vehicles/{licensePlate}` | → `VehicleResponse` | |
| GET | `/customers/{id}/service-history` | → `InvoiceSummary[]` | newest first |
| POST | `/customers/{id}/loyalty/redeem` | `{ "points": 50 }` → `RedeemResponse` | 422 if insufficient points |

### Module 2 — Employees & Dispatching

| Method | Path | Body → Response | Notes |
|---|---|---|---|
| POST | `/employees` | `EmployeeRequest` → `EmployeeResponse` | `type` = `MECHANIC` or `MANAGER` |
| GET | `/employees` | → `EmployeeResponse[]` | `?role=MECHANIC&specialization=Engine` |
| GET | `/employees/{id}` | → `EmployeeResponse` | includes `monthlySalary` from `calculateSalary()` |
| PUT | `/employees/{id}` | `EmployeeRequest` → `EmployeeResponse` | type cannot change |
| POST | `/mechanics/{id}/overtime` | `{ "hours": 5 }` → `EmployeeResponse` | calls `logOvertime` |
| POST | `/invoices/{invoiceId}/tasks` | `TaskRequest` → `TaskResponse` | creates job card, assigns mechanic |
| GET | `/tasks` | → `TaskResponse[]` | `?status=PENDING&mechanicId=...` |
| GET | `/tasks/{id}` | → `TaskResponse` | |
| POST | `/tasks/{id}/start` | → `TaskResponse` | PENDING → IN_PROGRESS |
| POST | `/tasks/{id}/complete` | → `TaskResponse` | IN_PROGRESS → COMPLETED via `Mechanic.completeServiceTask` |

### Module 3 — Inventory

| Method | Path | Body → Response | Notes |
|---|---|---|---|
| POST | `/inventory` | `InventoryItemRequest` → `InventoryItemResponse` | |
| GET | `/inventory` | → `InventoryItemResponse[]` | each carries `lowStock` |
| GET | `/inventory/{partCode}` | → `InventoryItemResponse` | |
| PUT | `/inventory/{partCode}` | `InventoryItemRequest` → `InventoryItemResponse` | cannot set stock directly; use restock or deduct |
| POST | `/inventory/{partCode}/restock` | `{ "quantity": 20 }` → `InventoryItemResponse` | |
| GET | `/inventory/low-stock` | → `InventoryItemResponse[]` | |
| GET | `/inventory/report?managerId=` | → `{ "report": "..." }` | `Manager.generateInventoryReport()` |

### Module 4 — Diagnostics & consumption

| Method | Path | Body → Response | Notes |
|---|---|---|---|
| PATCH | `/tasks/{id}/diagnostics` | `{ "description": "..." }` → `TaskResponse` | updates findings text while not COMPLETED |
| POST | `/tasks/{id}/parts` | `{ "partCode": "...", "quantity": 2 }` → `TaskResponse` | `ServiceTask.addUsedPart`; 409 on low stock |
| POST | `/tasks/{id}/labor` | `{ "billableHours": 1.5 }` → `TaskResponse` | applies configured hourly rate |

### Module 5 — Billing

| Method | Path | Body → Response | Notes |
|---|---|---|---|
| POST | `/invoices` | `{ "customerId": "...", "licensePlate": "..." }` → `InvoiceResponse` | opens invoice; vehicle must belong to the customer |
| GET | `/invoices` | → `InvoiceSummary[]` | `?status=` |
| GET | `/invoices/{id}` | → `InvoiceResponse` | full detail with tasks |
| POST | `/invoices/{id}/discount/percentage` | `{ "percentage": 10, "managerId": null }` → `InvoiceResponse` | overload 1; `managerId` required above the unapproved cap, then goes through `Manager.approveDiscount` |
| POST | `/invoices/{id}/discount/coupon` | `{ "couponCode": "WELCOME5", "fixedReduction": 5.0 }` → `InvoiceResponse` | overload 2 |
| PUT | `/invoices/{id}/tax` | `{ "taxRate": 8.0 }` → `InvoiceResponse` | OPEN only |
| POST | `/invoices/{id}/issue` | → `InvoiceResponse` | freezes totals; all tasks must be COMPLETED |
| POST | `/invoices/{id}/settle` | → `InvoiceResponse` | PAID; awards loyalty points |
| GET | `/invoices/{id}/receipt` | → `text/plain` | `generateReceipt()` output |

### Meta

| Method | Path | Notes |
|---|---|---|
| GET | `/vehicle-types` | `[{ "type": "CAR", "baseServiceFee": 50.00 }, ...]` built by instantiating each subclass and calling `getBaseServiceFee()` (live polymorphism demo) |
| GET | `/health` | Spring Actuator `/actuator/health` is fine |

## 4. DTOs (Java records) and TypeScript mirrors

Authors: write the Java record first, then copy the shape into TypeScript. Optional Java fields use `null`; TypeScript marks them `| null`.

```ts
// frontend/src/types/index.ts

export type VehicleType = 'CAR' | 'VAN' | 'MOTORCYCLE';
export type EmployeeType = 'MECHANIC' | 'MANAGER';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
export type InvoiceStatus = 'OPEN' | 'ISSUED' | 'PAID';

export interface ICustomerRequest { name: string; phone: string; email: string; }
export interface ICustomer {
  id: string; name: string; phone: string; email: string;
  loyaltyPoints: number; vehicleCount: number; createdAt: string;
}

export interface IVehicleRequest {
  type: VehicleType; licensePlate: string; brand: string; model: string;
  seatingCapacity?: number | null;   // required when CAR
  cargoVolume?: number | null;       // required when VAN
  engineCC?: number | null;          // required when MOTORCYCLE
}
export interface IVehicle {
  licensePlate: string; type: VehicleType; brand: string; model: string;
  baseServiceFee: number; ownerId: string;
  seatingCapacity: number | null; cargoVolume: number | null; engineCC: number | null;
}

export interface IEmployeeRequest {
  type: EmployeeType; name: string; phone: string; email: string; baseSalary: number;
  specialization?: string | null; hourlyRate?: number | null;   // MECHANIC
  monthlyBonus?: number | null;                                  // MANAGER
}
export interface IEmployee {
  id: string; type: EmployeeType; name: string; phone: string; email: string;
  baseSalary: number; monthlySalary: number;
  specialization: string | null; overtimeHours: number | null; hourlyRate: number | null;
  monthlyBonus: number | null;
}

export interface IInventoryItemRequest {
  partCode: string; partName: string; unitPrice: number;
  stockQuantity: number; reorderThreshold: number;   // stockQuantity honoured on create only
}
export interface IInventoryItem {
  partCode: string; partName: string; unitPrice: number;
  stockQuantity: number; reorderThreshold: number; lowStock: boolean;
  lastRestockedAt: string | null;
}

export interface ITaskRequest { description: string; assignedMechanicId: string; }
export interface IUsedPart {
  partCode: string; partName: string; quantity: number; unitPrice: number; subtotal: number;
}
export interface ITask {
  taskId: string; invoiceId: string; description: string; status: TaskStatus;
  assignedMechanicId: string; assignedMechanicName: string;
  billableHours: number; laborCharge: number; partsCost: number; totalTaskCost: number;
  usedParts: IUsedPart[]; createdAt: string;
}

export interface IInvoiceSummary {
  invoiceId: string; customerName: string; licensePlate: string;
  status: InvoiceStatus; totalAmount: number; issuedAt: string;
}
export interface IInvoice {
  invoiceId: string; customerId: string; customerName: string;
  licensePlate: string; vehicleType: VehicleType; baseServiceFee: number;
  tasks: ITask[]; subtotal: number; discountAmount: number;
  taxRate: number; taxAmount: number; totalAmount: number;
  status: InvoiceStatus; issuedAt: string; settledAt: string | null;
}
export interface IRedeemResponse { redeemed: boolean; remainingPoints: number; }

export interface IApiError {
  timestamp: string; status: number; error: string; code: string;
  message: string; path: string; fieldErrors?: { field: string; message: string }[];
}
```

Naming: the `I` prefix follows the proposal (`IVehicle`, `ICustomer`, `IInvoice`). Java records drop the prefix: `VehicleResponse`, `CustomerResponse`, `InvoiceResponse`, and so on.

Mapping Java to TS names:

| Java record | TS interface |
|---|---|
| `CustomerRequest` / `CustomerResponse` | `ICustomerRequest` / `ICustomer` |
| `VehicleRequest` / `VehicleResponse` | `IVehicleRequest` / `IVehicle` |
| `EmployeeRequest` / `EmployeeResponse` | `IEmployeeRequest` / `IEmployee` |
| `InventoryItemRequest` / `InventoryItemResponse` | `IInventoryItemRequest` / `IInventoryItem` |
| `TaskRequest` / `TaskResponse` | `ITaskRequest` / `ITask` |
| `InvoiceSummary` / `InvoiceResponse` | `IInvoiceSummary` / `IInvoice` |
| `RedeemResponse` | `IRedeemResponse` |
| `ApiError` | `IApiError` |

## 5. Polymorphic request handling

`VehicleRequest` and `EmployeeRequest` carry `type`. The service uses a small factory (`VehicleFactory`, `EmployeeFactory`) that builds the right subclass. Do not branch on type anywhere else. After creation, everything works through `Vehicle` and `Employee` references.

## 6. Service layer public API (for agents writing controllers or tests)

```java
CustomerService   create, update, delete, get, list, registerVehicle, redeemPoints, serviceHistory
EmployeeService   create, update, get, list, logOvertime
InventoryService  create, update, get, list, restock, lowStock, report(managerId)
TaskService       create(invoiceId, req), start, complete, updateDiagnostics, addPart, recordLabor, get, list
InvoiceService    open, get, list, applyPercentageDiscount, applyCouponDiscount, setTax, issue, settle, receipt
```

Services are `@Transactional`, load aggregates, call domain methods, save, and map to DTOs. A `TaskService.addPart` call must run in one transaction so stock deduction and the used-part row commit or roll back together.

## 7. Frontend integration notes

- `frontend/src/api/client.ts`: a thin `fetch` wrapper that reads `VITE_API_BASE_URL`, throws `IApiError` on non-2xx.
- One API module per backend module (`customers.ts`, `vehicles.ts`, `employees.ts`, `tasks.ts`, `inventory.ts`, `invoices.ts`) with fully typed function signatures.
- Pages: Dashboard, Customers (+ detail with vehicles and history), Employees, Task Board (three columns by status), Inventory (low-stock highlight), Invoices (detail, discount, issue, settle, receipt view).
- No `any`. `strict: true` in `tsconfig.json`.
