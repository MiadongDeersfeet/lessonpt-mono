import { ApiError } from './apiClient.ts'
import type { ErrorResponse } from '../types/api.ts'
import type { StudentMe, StudentPortalLearning } from '../types/studentPortal.ts'

const apiBase = import.meta.env.VITE_API_BASE_URL ?? '/api'

export function requestStudentOtp(publicAccessKey: string, email: string): Promise<void> {
  return studentRequest<void>(`/v1/student-access/${encodeURIComponent(publicAccessKey)}/otp`, {
    method: 'POST',
    body: { email },
  })
}

export function verifyStudentOtp(publicAccessKey: string, email: string, otp: string): Promise<{ verified: boolean }> {
  return studentRequest<{ verified: boolean }>(`/v1/student-access/${encodeURIComponent(publicAccessKey)}/otp/verify`, {
    method: 'POST',
    body: { email, otp },
  })
}

export function getStudentMe(): Promise<StudentMe> {
  return studentRequest<StudentMe>('/v1/student/me')
}

export function getStudentLearning(): Promise<StudentPortalLearning> {
  return studentRequest<StudentPortalLearning>('/v1/student/learning')
}

export function logoutStudent(): Promise<void> {
  return studentRequest<void>('/v1/student/session/logout', { method: 'POST' })
}

async function studentRequest<T>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(buildUrl(path), {
      method: options.method ?? 'GET',
      credentials: 'include',
      headers: headers(options.body !== undefined),
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결하지 못했습니다.', null, [])
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

function buildUrl(path: string): string {
  const base = apiBase.endsWith('/') ? apiBase.slice(0, -1) : apiBase
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${base}${suffix}`
}

function headers(hasBody: boolean): HeadersInit {
  const result: Record<string, string> = { Accept: 'application/json' }
  if (hasBody) {
    result['Content-Type'] = 'application/json'
  }
  return result
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
