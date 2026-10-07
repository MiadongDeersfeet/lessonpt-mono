import type { AuthTokenResponse, LoginRequest, SignupRequest, TeacherMe, TeacherSignup } from '../types/auth.ts'
import { apiRequest } from './apiClient.ts'

export function signup(request: SignupRequest): Promise<TeacherSignup> {
  return apiRequest<TeacherSignup>('/v1/auth/signup', {
    method: 'POST',
    body: request,
    auth: false,
  })
}

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
