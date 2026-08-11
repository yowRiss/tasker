import { AuthService } from './authService';

export interface ProblemDetails {
  type?: string;
  title?: string;
  status: number;
  code?: string;
  detail?: string;
  request_id?: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  status: number;
  code?: string;
  problem: ProblemDetails;

  constructor(problem: ProblemDetails) {
    super(problem.detail || problem.title || `API Error ${problem.status}`);
    this.name = 'ApiError';
    this.status = problem.status;
    this.code = problem.code;
    this.problem = problem;
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const baseUrl = await AuthService.getApiUrl();
  const token = await AuthService.getToken();

  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const url = `${baseUrl}${path.startsWith('/') ? path : '/' + path}`;

  let response: Response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch (err: any) {
    throw new ApiError({
      status: 0,
      code: 'network_error',
      title: 'Network Error',
      detail: err.message || 'Failed to connect to backend server',
    });
  }

  if (response.status === 204) {
    return {} as T;
  }

  let body: any;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
    body = await response.json();
  } else {
    body = await response.text();
  }

  if (!response.ok) {
    const problem: ProblemDetails = typeof body === 'object' ? body : {
      status: response.status,
      title: response.statusText,
      detail: String(body),
    };
    throw new ApiError(problem);
  }

  return body as T;
}
