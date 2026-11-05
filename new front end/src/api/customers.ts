import { request } from './client';
import type {
  ICustomer,
  ICustomerRequest,
  IVehicle,
  IVehicleRequest,
  IInvoiceSummary,
  IRedeemResponse,
} from '../types';

export const customersApi = {
  list: (query?: string): Promise<ICustomer[]> => {
    const q = query ? `?q=${encodeURIComponent(query)}` : '';
    return request<ICustomer[]>(`/customers${q}`);
  },

  get: (id: string): Promise<ICustomer> => {
    return request<ICustomer>(`/customers/${id}`);
  },

  create: (data: ICustomerRequest): Promise<ICustomer> => {
    return request<ICustomer>('/customers', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  update: (id: string, data: ICustomerRequest): Promise<ICustomer> => {
    return request<ICustomer>(`/customers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  delete: (id: string): Promise<void> => {
    return request<void>(`/customers/${id}`, {
      method: 'DELETE',
    });
  },

  getVehicles: (customerId: string): Promise<IVehicle[]> => {
    return request<IVehicle[]>(`/customers/${customerId}/vehicles`);
  },

  registerVehicle: (customerId: string, data: IVehicleRequest): Promise<IVehicle> => {
    return request<IVehicle>(`/customers/${customerId}/vehicles`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getServiceHistory: (customerId: string): Promise<IInvoiceSummary[]> => {
    return request<IInvoiceSummary[]>(`/customers/${customerId}/service-history`);
  },

  redeemLoyalty: (customerId: string, points: number): Promise<IRedeemResponse> => {
    return request<IRedeemResponse>(`/customers/${customerId}/loyalty/redeem`, {
      method: 'POST',
      body: JSON.stringify({ points }),
    });
  },
};
