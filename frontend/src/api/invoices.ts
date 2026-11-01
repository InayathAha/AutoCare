import { request } from './client';
import type {
  IInvoice,
  IInvoiceSummary,
  IOpenInvoiceRequest,
  IDiscountPercentageRequest,
  IDiscountCouponRequest,
} from '../types';

export const invoicesApi = {
  open: (data: IOpenInvoiceRequest): Promise<IInvoice> => {
    return request<IInvoice>('/invoices', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  list: (status?: string): Promise<IInvoiceSummary[]> => {
    const q = status ? `?status=${encodeURIComponent(status)}` : '';
    return request<IInvoiceSummary[]>(`/invoices${q}`);
  },

  get: (id: string): Promise<IInvoice> => {
    return request<IInvoice>(`/invoices/${id}`);
  },

  applyPercentageDiscount: (id: string, data: IDiscountPercentageRequest): Promise<IInvoice> => {
    return request<IInvoice>(`/invoices/${id}/discount/percentage`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  applyCouponDiscount: (id: string, data: IDiscountCouponRequest): Promise<IInvoice> => {
    return request<IInvoice>(`/invoices/${id}/discount/coupon`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  setTaxRate: (id: string, taxRate: number): Promise<IInvoice> => {
    return request<IInvoice>(`/invoices/${id}/tax`, {
      method: 'PUT',
      body: JSON.stringify({ taxRate }),
    });
  },

  issue: (id: string): Promise<IInvoice> => {
    return request<IInvoice>(`/invoices/${id}/issue`, {
      method: 'POST',
    });
  },

  settle: (id: string): Promise<IInvoice> => {
    return request<IInvoice>(`/invoices/${id}/settle`, {
      method: 'POST',
    });
  },

  getReceipt: (id: string): Promise<string> => {
    return request<string>(`/invoices/${id}/receipt`);
  },
};
