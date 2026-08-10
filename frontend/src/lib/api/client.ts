const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined
const baseUrl = configuredBaseUrl?.replace(/\/$/, '')

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message)
  }
}

export function getToken(): string | null {
  return localStorage.getItem('tasker_token')
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem('tasker_token', token)
  } else {
    localStorage.removeItem('tasker_token')
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (baseUrl === undefined) throw new Error('VITE_API_BASE_URL is required.')
  const jwt = getToken()
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(init.headers as Record<string, string>),
  }
  if (jwt) {
    headers['Authorization'] = `Bearer ${jwt}`
  }
  if (init.body && typeof init.body === 'string' && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers,
  })

  if (response.status === 401 && path !== '/v1/auth/login') {
    setToken(null)
    window.location.reload()
    throw new ApiError('Your session has ended. Please sign in again.', 401)
  }

  if (response.status === 204) return undefined as T

  if (!response.ok) {
    const problem: unknown = await response.json().catch(() => null)
    const details =
      problem && typeof problem === 'object'
        ? (problem as { title?: unknown; detail?: unknown; code?: unknown })
        : {}
    throw new ApiError(
      typeof details.detail === 'string'
        ? details.detail
        : typeof details.title === 'string'
          ? details.title
          : 'The request could not be completed.',
      response.status,
      typeof details.code === 'string' ? details.code : undefined,
    )
  }
  return response.json() as Promise<T>
}

export function query(params: Record<string, string | number | boolean | undefined | null>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const result = search.toString()
  return result ? `?${result}` : ''
}
