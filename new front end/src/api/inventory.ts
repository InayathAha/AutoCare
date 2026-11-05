import { request } from './client';
import type { IInventoryItem, IInventoryItemRequest, IReportResponse } from '../types';

export const inventoryApi = {
  list: (): Promise<IInventoryItem[]> => {
    return request<IInventoryItem[]>('/inventory');
  },

  get: (partCode: string): Promise<IInventoryItem> => {
    return request<IInventoryItem>(`/inventory/${encodeURIComponent(partCode)}`);
  },

  create: (data: IInventoryItemRequest): Promise<IInventoryItem> => {
    return request<IInventoryItem>('/inventory', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  update: (partCode: string, data: IInventoryItemRequest): Promise<IInventoryItem> => {
    return request<IInventoryItem>(`/inventory/${encodeURIComponent(partCode)}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  restock: (partCode: string, quantity: number): Promise<IInventoryItem> => {
    return request<IInventoryItem>(`/inventory/${encodeURIComponent(partCode)}/restock`, {
      method: 'POST',
      body: JSON.stringify({ quantity }),
    });
  },

  listLowStock: (): Promise<IInventoryItem[]> => {
    return request<IInventoryItem[]>('/inventory/low-stock');
  },

  getReport: (managerId: string): Promise<IReportResponse> => {
    return request<IReportResponse>(`/inventory/report?managerId=${encodeURIComponent(managerId)}`);
  },
};
