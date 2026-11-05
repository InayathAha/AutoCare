// frontend/src/types/index.ts

export type VehicleType = 'CAR' | 'VAN' | 'MOTORCYCLE';
export type EmployeeType = 'MECHANIC' | 'MANAGER';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
export type InvoiceStatus = 'OPEN' | 'ISSUED' | 'PAID';

export interface ICustomerRequest {
  name: string;
  phone: string;
  email: string;
}

export interface ICustomer {
  id: string;
  name: string;
  phone: string;
  email: string;
  loyaltyPoints: number;
  vehicleCount: number;
  createdAt: string;
}

export interface IVehicleRequest {
  type: VehicleType;
  licensePlate: string;
  brand: string;
  model: string;
  seatingCapacity?: number | null;   // required when CAR
  cargoVolume?: number | null;       // required when VAN
  engineCC?: number | null;          // required when MOTORCYCLE
}

export interface IVehicle {
  licensePlate: string;
  type: VehicleType;
  brand: string;
  model: string;
  baseServiceFee: number;
  ownerId: string;
  seatingCapacity: number | null;
  cargoVolume: number | null;
  engineCC: number | null;
}

export interface IEmployeeRequest {
  type: EmployeeType;
  name: string;
  phone: string;
  email: string;
  baseSalary: number;
  specialization?: string | null;    // MECHANIC
  hourlyRate?: number | null;        // MECHANIC
  monthlyBonus?: number | null;      // MANAGER
}

export interface IEmployee {
  id: string;
  type: EmployeeType;
  name: string;
  phone: string;
  email: string;
  baseSalary: number;
  monthlySalary: number;
  specialization: string | null;
  overtimeHours: number | null;
  hourlyRate: number | null;
  monthlyBonus: number | null;
}

export interface IInventoryItemRequest {
  partCode: string;
  partName: string;
  unitPrice: number;
  stockQuantity: number;
  reorderThreshold: number;          // stockQuantity honoured on create only
}

export interface IInventoryItem {
  partCode: string;
  partName: string;
  unitPrice: number;
  stockQuantity: number;
  reorderThreshold: number;
  lowStock: boolean;
  lastRestockedAt: string | null;
}

export interface ITaskRequest {
  description: string;
  assignedMechanicId: string;
}

export interface IUsedPart {
  partCode: string;
  partName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface ITask {
  taskId: string;
  invoiceId: string;
  description: string;
  status: TaskStatus;
  assignedMechanicId: string;
  assignedMechanicName: string;
  billableHours: number;
  laborCharge: number;
  partsCost: number;
  totalTaskCost: number;
  usedParts: IUsedPart[];
  createdAt: string;
}

export interface IInvoiceSummary {
  invoiceId: string;
  customerName: string;
  licensePlate: string;
  status: InvoiceStatus;
  totalAmount: number;
  issuedAt: string;
}

export interface IInvoice {
  invoiceId: string;
  customerId: string;
  customerName: string;
  licensePlate: string;
  vehicleType: VehicleType;
  baseServiceFee: number;
  tasks: ITask[];
  subtotal: number;
  discountAmount: number;
  taxRate: number;
  taxAmount: number;
  totalAmount: number;
  status: InvoiceStatus;
  issuedAt: string;
  settledAt: string | null;
}

export interface IRedeemResponse {
  redeemed: boolean;
  remainingPoints: number;
}

export interface IOpenInvoiceRequest {
  customerId: string;
  licensePlate: string;
}

export interface IDiscountPercentageRequest {
  percentage: number;
  managerId?: string | null;
}

export interface IDiscountCouponRequest {
  couponCode: string;
  fixedReduction: number;
}

export interface ITaxRateRequest {
  taxRate: number;
}

export interface ITaskPartRequest {
  partCode: string;
  quantity: number;
}

export interface ILaborRequest {
  billableHours: number;
}

export interface IDiagnosticsRequest {
  description: string;
}

export interface IOvertimeRequest {
  hours: number;
}

export interface IRestockRequest {
  quantity: number;
}

export interface IVehicleTypeInfo {
  type: VehicleType;
  baseServiceFee: number;
}

export interface IReportResponse {
  report: string;
}

export interface IApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}
