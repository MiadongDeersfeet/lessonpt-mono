import { afterEach, expect, it, vi } from 'vitest'
import { saveTokens } from '../auth/tokenStore.ts'
import { getStudentMe } from './studentPortalApi.ts'

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

  await expect(getStudentMe()).rejects.toMatchObject({ status: 401 })

  expect(fetchMock).toHaveBeenCalledTimes(1)
  const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
  expect(url).toBe('/api/v1/student/me')
  expect(init.credentials).toBe('include')
  expect(init.headers).not.toHaveProperty('Authorization')
  expect(sessionStorage.getItem('lessonpt.teacher.refreshToken')).toBe('teacher-refresh')
})
