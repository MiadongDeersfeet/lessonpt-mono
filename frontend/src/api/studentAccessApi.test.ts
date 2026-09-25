import { afterEach, expect, it, vi } from 'vitest'
import { saveTokens } from '../auth/tokenStore.ts'
import { createStudentAccess, deleteStudentAccess, getStudentAccess } from './studentApi.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  sessionStorage.clear()
})

it('calls student access through the teacher api client', async () => {
  saveTokens({ accessToken: 'teacher-access', refreshToken: 'teacher-refresh', accessTokenExpiresIn: 60 })
  const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
    if (init?.method === 'DELETE') {
      return new Response(null, { status: 204 })
    }
    return new Response(
      JSON.stringify({
        teacherStudentAccessId: 90,
        teacherStudentId: 72,
        publicAccessKey: 'new-key',
        createdAt: null,
        status: 'ACTIVE',
      }),
      { status: init?.method === 'POST' ? 201 : 200, headers: { 'Content-Type': 'application/json' } },
    )
  })
  vi.stubGlobal('fetch', fetchMock)

  await getStudentAccess(41)
  await createStudentAccess(41)
  await deleteStudentAccess(41)

  const urls = fetchMock.mock.calls.map((call) => String(call[0]))
  expect(urls).toEqual([
    '/api/v1/students/41/access',
    '/api/v1/students/41/access',
    '/api/v1/students/41/access',
  ])
  expect(urls.join(' ')).not.toContain('/student-access/')
  expect(urls.join(' ')).not.toContain('/student-login/')
  expect(urls.join(' ')).not.toContain('/auth/refresh')
  for (const call of fetchMock.mock.calls) {
    const init = call[1] as RequestInit
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer teacher-access')
    expect(init.credentials).toBeUndefined()
  }
  expect((fetchMock.mock.calls[1][1] as RequestInit).method).toBe('POST')
  expect((fetchMock.mock.calls[1][1] as RequestInit).body).toBeUndefined()
  expect((fetchMock.mock.calls[2][1] as RequestInit).method).toBe('DELETE')
})
