import type { AuthTokenResponse, LoginRequest, TeacherMe } from '../types/auth.ts'
import { apiRequest } from './apiClient.ts'

export function login(request: LoginRequest): Promise<AuthTokenResponse> {
  return apiRequest<AuthTokenResponse>('/v1/auth/login', {
    method: 'POST',
    body: request,
    auth: false,
  })
}

export function logout(): Promise<void> {
  return apiRequest<void>('/v1/auth/logout', { method: 'POST' })
}

export function me(): Promise<TeacherMe> {
  return apiRequest<TeacherMe>('/v1/teachers/me')
}
