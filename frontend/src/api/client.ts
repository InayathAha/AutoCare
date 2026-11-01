import type { IApiError } from '../types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiErrorResponse extends Error {
  error: IApiError;

  constructor(error: IApiError) {
    super(error.message || 'API request failed');
    this.name = 'ApiErrorResponse';
    this.error = error;
  }
}

export async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let apiError: IApiError;
    try {
      apiError = (await response.json()) as IApiError;
    } catch {
      apiError = {
        timestamp: new Date().toISOString(),
        status: response.status,
        error: response.statusText,
        code: 'INTERNAL_ERROR',
        message: `HTTP error ${response.status}: ${response.statusText}`,
        path: endpoint,
      };
    }
    throw new ApiErrorResponse(apiError);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('Content-Type') || '';
  if (contentType.includes('text/plain')) {
    const text = await response.text();
    return text as unknown as T;
  }

  return response.json() as Promise<T>;
}
