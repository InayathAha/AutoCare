import { request } from './client';
import type { IEmployee, IEmployeeRequest } from '../types';

export const employeesApi = {
  list: (role?: string, specialization?: string): Promise<IEmployee[]> => {
    const params = new URLSearchParams();
    if (role) params.set('role', role);
    if (specialization) params.set('specialization', specialization);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return request<IEmployee[]>(`/employees${qs}`);
  },

  get: (id: string): Promise<IEmployee> => {
    return request<IEmployee>(`/employees/${id}`);
  },

  create: (data: IEmployeeRequest): Promise<IEmployee> => {
    return request<IEmployee>('/employees', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  update: (id: string, data: IEmployeeRequest): Promise<IEmployee> => {
    return request<IEmployee>(`/employees/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  logOvertime: (mechanicId: string, hours: number): Promise<IEmployee> => {
    return request<IEmployee>(`/mechanics/${mechanicId}/overtime`, {
      method: 'POST',
      body: JSON.stringify({ hours }),
    });
  },
};
