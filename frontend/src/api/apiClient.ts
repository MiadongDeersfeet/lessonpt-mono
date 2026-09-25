import { notifySessionLost } from '../auth/sessionEvents.ts'
import { clearTokens, readTokens, saveTokens } from '../auth/tokenStore.ts'
import type { ErrorResponse, FieldError } from '../types/api.ts'
import type { AuthTokenResponse } from '../types/auth.ts'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly traceId: string | null
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, traceId: string | null, fieldErrors: FieldError[]) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.traceId = traceId
    this.fieldErrors = fieldErrors
  }
}

type RequestOptions = {
  method?: string
  body?: unknown
  auth?: boolean
  retried?: boolean
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api'

let refreshInFlight: Promise<boolean> | null = null

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const useAuth = options.auth !== false
  let response: Response
  try {
    response = await fetch(buildUrl(path), {
      method: options.method ?? 'GET',
      headers: headers(useAuth, options.body !== undefined),
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결하지 못했습니다.', null, [])
  }

  if (response.status === 401 && useAuth && !options.retried) {
    const refreshed = await refreshOnce()
    if (refreshed) {
      return apiRequest<T>(path, { ...options, retried: true })
    }
    clearTokens()
    notifySessionLost()
  }

  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export async function apiSend(path: string, options: { method?: string; body?: BodyInit; retried?: boolean } = {}): Promise<Response> {
  let response: Response
  try {
    response = await fetch(buildUrl(path), {
      method: options.method ?? 'GET',
      headers: authHeaders(),
      body: options.body,
    })
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결하지 못했습니다.', null, [])
  }
  if (response.status === 401 && !options.retried) {
    const refreshed = await refreshOnce()
    if (refreshed) {
      return apiSend(path, { ...options, retried: true })
    }
    clearTokens()
    notifySessionLost()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  return response
}

function buildUrl(path: string): string {
  const base = apiBase.endsWith('/') ? apiBase.slice(0, -1) : apiBase
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${base}${suffix}`
}

function headers(useAuth: boolean, hasBody: boolean): HeadersInit {
  const result = authHeaders(useAuth)
  if (hasBody) {
    result['Content-Type'] = 'application/json'
  }
  return result
}

function authHeaders(useAuth = true): Record<string, string> {
  const result: Record<string, string> = { Accept: 'application/json' }
  if (useAuth) {
    const accessToken = readTokens()?.accessToken
    if (accessToken) {
      result.Authorization = `Bearer ${accessToken}`
    }
  }
  return result
}

function refreshOnce(): Promise<boolean> {
  const refreshToken = readTokens()?.refreshToken
  if (!refreshToken) {
    return Promise.resolve(false)
  }
  if (!refreshInFlight) {
    refreshInFlight = refresh(refreshToken).finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function refresh(refreshToken: string): Promise<boolean> {
  let response: Response
  try {
    response = await fetch(buildUrl('/v1/auth/refresh'), {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
  } catch {
    return false
  }
  if (!response.ok) {
    return false
  }
  const tokens = (await response.json()) as AuthTokenResponse
  saveTokens(tokens)
  return true
}

async function toApiError(response: Response): Promise<ApiError> {
  const payload = await readErrorBody(response)
  return new ApiError(
    payload?.status ?? response.status,
    payload?.code ?? 'UNKNOWN',
    payload?.message ?? '요청을 처리하지 못했습니다.',
    payload?.traceId ?? null,
    payload?.fieldErrors ?? [],
  )
}

async function readErrorBody(response: Response): Promise<ErrorResponse | null> {
  try {
    return (await response.json()) as ErrorResponse
  } catch {
    return null
  }
}
