import { afterEach, expect, it, vi } from 'vitest'
import { saveTokens } from '../auth/tokenStore.ts'
import {
  clearStudentScope,
  getStudentMe,
  isStudentScopeRequired,
  requestStudentLoginOtp,
  selectStudentScope,
  verifyStudentLoginOtp,
} from './studentPortalApi.ts'
import { ApiError } from './apiClient.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  sessionStorage.clear()
})

it('sends the student cookie and does not refresh a teacher token', async () => {
  saveTokens({ accessToken: 'teacher-access', refreshToken: 'teacher-refresh', accessTokenExpiresIn: 60 })
  const fetchMock = vi.fn().mockResolvedValue({
    ok: false,
    status: 401,
    json: async () => ({
      status: 401,
      code: 'AUTH_FAILED',
      message: '인증에 실패했습니다.',
      traceId: 'trace-s',
      fieldErrors: [],
    }),
  })
  vi.stubGlobal('fetch', fetchMock)

  await expect(getStudentMe()).rejects.toMatchObject({ status: 401, code: 'AUTH_FAILED' })

  expect(fetchMock).toHaveBeenCalledTimes(1)
  const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
  expect(url).toBe('/api/v1/student/me')
  expect(init.credentials).toBe('include')
  expect(init.headers).not.toHaveProperty('Authorization')
  expect(String(url)).not.toContain('/auth/refresh')
  expect(sessionStorage.getItem('lessonpt.teacher.refreshToken')).toBe('teacher-refresh')
  expect(sessionStorage.getItem('lessonpt.teacher.accessToken')).toBe('teacher-access')
})

it('posts general login and scope commands with the cookie and without a bearer token', async () => {
  const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 204 })
  vi.stubGlobal('fetch', fetchMock)

  await requestStudentLoginOtp('student@example.com')
  await verifyStudentLoginOtp('student@example.com', '123456')
  await selectStudentScope(11)
  await clearStudentScope()

  const urls = fetchMock.mock.calls.map((call) => String(call[0]))
  expect(urls).toEqual([
    '/api/v1/student-login/otp',
    '/api/v1/student-login/otp/verify',
    '/api/v1/student/session/scope',
    '/api/v1/student/session/scope',
  ])
  for (const call of fetchMock.mock.calls) {
    const init = call[1] as RequestInit
    expect(init.credentials).toBe('include')
    expect(init.headers).not.toHaveProperty('Authorization')
  }
  expect((fetchMock.mock.calls[2][1] as RequestInit).body).toBe(JSON.stringify({ teacherStudentAccessId: 11 }))
  expect((fetchMock.mock.calls[3][1] as RequestInit).method).toBe('DELETE')
  expect(urls.join(' ')).not.toContain('/auth/refresh')
})

it('identifies a missing student scope separately from auth failure', () => {
  expect(isStudentScopeRequired(new ApiError(409, 'STUDENT_SCOPE_REQUIRED', '수업 범위를 먼저 선택해 주세요.', null, []))).toBe(true)
  expect(isStudentScopeRequired(new ApiError(401, 'AUTH_FAILED', '인증에 실패했습니다.', null, []))).toBe(false)
})
