import { request } from './client';
import type { IVehicle, IVehicleTypeInfo } from '../types';

export const vehiclesApi = {
  get: (licensePlate: string): Promise<IVehicle> => {
    return request<IVehicle>(`/vehicles/${encodeURIComponent(licensePlate)}`);
  },

  getTypes: (): Promise<IVehicleTypeInfo[]> => {
    return request<IVehicleTypeInfo[]>('/vehicle-types');
  },
};
