import type { IApiError } from '../types';

// Resolve API base URL across Vercel service bindings, Vite build variables, and local dev
function resolveApiBaseUrl(): string {
  // 1. Runtime service binding injected by Vercel (process.env.API_URL)
  if (typeof process !== 'undefined' && process.env?.API_URL) {
    const bound = process.env.API_URL.replace(/\/+$/, '');
    return `${bound}/api/v1`;
  }
  // 2. Build-time or deployment environment variable
  if (import.meta.env?.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  // 3. Browser environment on Vercel deployment: use relative path to route through Vercel /api rewrite
  if (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    return '/api/v1';
  }
  // 4. Local development default
  return 'http://localhost:8080/api/v1';
}

const rawBaseUrl = resolveApiBaseUrl();

export function joinUrl(base: string, endpoint: string): string {
  const cleanBase = base.replace(/\/+$/, '');
  const cleanEndpoint = endpoint.replace(/^\/+/, '');
  return `${cleanBase}/${cleanEndpoint}`;
}

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
  const url = joinUrl(rawBaseUrl, endpoint);

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    const apiError: IApiError = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: 'Network Error',
      code: 'INTERNAL_ERROR',
      message: `Failed to connect to ${url}: ${errorMsg}`,
      path: endpoint,
    };
    throw new ApiErrorResponse(apiError);
  }

  const contentType = response.headers.get('content-type') || '';
  const isJson = contentType.includes('application/json');

  if (!response.ok) {
    let apiError: IApiError;
    if (isJson) {
      try {
        apiError = (await response.json()) as IApiError;
      } catch {
        apiError = {
          timestamp: new Date().toISOString(),
          status: response.status,
          error: response.statusText || 'Error',
          code: 'INTERNAL_ERROR',
          message: `HTTP error ${response.status} from ${url} (failed to parse JSON)`,
          path: endpoint,
        };
      }
    } else {
      apiError = {
        timestamp: new Date().toISOString(),
        status: response.status,
        error: response.statusText || 'HTTP Error',
        code: response.status === 404 ? 'NOT_FOUND' : 'INTERNAL_ERROR',
        message: `HTTP error ${response.status} from ${url} (non-JSON response)`,
        path: endpoint,
      };
    }
    throw new ApiErrorResponse(apiError);
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  if (contentType.includes('text/plain')) {
    const text = await response.text();
    return text as unknown as T;
  }

  if (!isJson) {
    const apiError: IApiError = {
      timestamp: new Date().toISOString(),
      status: response.status,
      error: 'Invalid Content-Type',
      code: 'INTERNAL_ERROR',
      message: `HTTP error ${response.status} from ${url} (expected JSON, received '${contentType || 'unknown'}')`,
      path: endpoint,
    };
    throw new ApiErrorResponse(apiError);
  }

  try {
    return (await response.json()) as T;
  } catch {
    const apiError: IApiError = {
      timestamp: new Date().toISOString(),
      status: response.status,
      error: 'JSON Parse Error',
      code: 'INTERNAL_ERROR',
      message: `HTTP error ${response.status} from ${url} (malformed JSON response)`,
      path: endpoint,
    };
    throw new ApiErrorResponse(apiError);
  }
}
