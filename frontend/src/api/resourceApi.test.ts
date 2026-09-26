import { afterEach, expect, it, vi } from 'vitest'
import { saveTokens, clearTokens } from '../auth/tokenStore.ts'
import { uploadContentResource, fetchStudentResourceBlob, studentResourceContentUrl } from './resourceApi.ts'

afterEach(() => {
  clearTokens()
  vi.unstubAllGlobals()
})

it('posts multipart form data without a manual content type', async () => {
  saveTokens({ accessToken: 'teacher-token', refreshToken: 'refresh', accessTokenExpiresIn: 3600 })
  const fetchMock = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => {
    return new Response(
      JSON.stringify({
        resourceId: 4,
        resourceType: 'SHEET',
        originalFileName: 'notes.pdf',
        contentType: 'application/pdf',
        fileSize: 12,
        createdAt: '2026-09-26T01:00:00',
      }),
      { status: 201, headers: { 'Content-Type': 'application/json', 'X-Lessonpt-Storage-Warning': 'true' } },
    )
  })
  vi.stubGlobal('fetch', fetchMock)
  const file = new File(['%PDF'], 'notes.pdf', { type: 'application/pdf' })

  const uploaded = await uploadContentResource(3, 8, 15, 'sheet', file)

  const init = fetchMock.mock.calls[0][1]
  if (!init?.body) {
    throw new Error('upload request was not sent')
  }
  expect(init.body).toBeInstanceOf(FormData)
  expect((init.body as FormData).get('file')).toBe(file)
  expect(new Headers(init.headers).get('Content-Type')).toBeNull()
  expect(new Headers(init.headers).get('Authorization')).toBe('Bearer teacher-token')
  expect(uploaded.storageWarning).toBe(true)
  expect(uploaded.resource.resourceId).toBe(4)
  expect(uploaded.resource).not.toHaveProperty('objectKey')
})

it('builds a student resource url without a bearer token', () => {
  expect(studentResourceContentUrl(9, 'inline')).toBe('/api/v1/student/resources/9/content?disposition=inline')
  expect(studentResourceContentUrl(9, 'attachment')).toBe('/api/v1/student/resources/9/content?disposition=attachment')
})

it('fetches a student pdf with the session cookie and without a bearer token', async () => {
  saveTokens({ accessToken: 'teacher-token', refreshToken: 'refresh', accessTokenExpiresIn: 3600 })
  const fetchMock = vi.fn(async (_input: RequestInfo | URL, _init?: RequestInit) => {
    return new Response('%PDF', { status: 200, headers: { 'Content-Type': 'application/pdf' } })
  })
  vi.stubGlobal('fetch', fetchMock)

  const blob = await fetchStudentResourceBlob(9, 'inline')

  expect(await blob.text()).toBe('%PDF')
  expect(fetchMock).toHaveBeenCalledWith('/api/v1/student/resources/9/content?disposition=inline', {
    credentials: 'include',
    headers: { Accept: 'application/pdf' },
  })
  const init = fetchMock.mock.calls[0][1]
  if (!init) {
    throw new Error('student pdf request was not sent')
  }
  expect(new Headers(init.headers).get('Authorization')).toBeNull()
})
