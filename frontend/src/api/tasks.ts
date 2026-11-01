import { request } from './client';
import type { ITask, ITaskRequest, ITaskPartRequest } from '../types';

export const tasksApi = {
  create: (invoiceId: string, data: ITaskRequest): Promise<ITask> => {
    return request<ITask>(`/invoices/${invoiceId}/tasks`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  list: (status?: string, mechanicId?: string): Promise<ITask[]> => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (mechanicId) params.set('mechanicId', mechanicId);
    const qs = params.toString() ? `?${params.toString()}` : '';
    return request<ITask[]>(`/tasks${qs}`);
  },

  get: (id: string): Promise<ITask> => {
    return request<ITask>(`/tasks/${id}`);
  },

  start: (id: string): Promise<ITask> => {
    return request<ITask>(`/tasks/${id}/start`, {
      method: 'POST',
    });
  },

  complete: (id: string): Promise<ITask> => {
    return request<ITask>(`/tasks/${id}/complete`, {
      method: 'POST',
    });
  },

  updateDiagnostics: (id: string, description: string): Promise<ITask> => {
    return request<ITask>(`/tasks/${id}/diagnostics`, {
      method: 'PATCH',
      body: JSON.stringify({ description }),
    });
  },

  addPart: (id: string, data: ITaskPartRequest): Promise<ITask> => {
    return request<ITask>(`/tasks/${id}/parts`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  recordLabor: (id: string, billableHours: number): Promise<ITask> => {
    return request<ITask>(`/tasks/${id}/labor`, {
      method: 'POST',
      body: JSON.stringify({ billableHours }),
    });
  },
};
