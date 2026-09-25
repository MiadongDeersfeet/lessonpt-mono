import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './apiClient.ts'
import { clearTokens, readTokens, saveTokens } from '../auth/tokenStore.ts'

afterEach(() => {
  clearTokens()
  vi.unstubAllGlobals()
})

describe('apiRequest', () => {
  it('parses ErrorResponse fields', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        jsonResponse(404, {
          timestamp: '2026-09-25T15:00:00+09:00',
          status: 404,
          code: 'COMMON_NOT_FOUND',
          message: '요청한 대상을 찾을 수 없습니다.',
          path: '/api/v1/students/9',
          traceId: 'trace-1',
          fieldErrors: [],
        }),
      ),
    )

    const error = await apiRequest('/v1/students/9').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    const apiError = error as ApiError
    expect(apiError.status).toBe(404)
    expect(apiError.code).toBe('COMMON_NOT_FOUND')
    expect(apiError.traceId).toBe('trace-1')
    expect(apiError.fieldErrors).toEqual([])
  })

  it('refreshes once and retries the original request', async () => {
    saveTokens({ accessToken: 'old', refreshToken: 'refresh-1', accessTokenExpiresIn: 3600 })
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.endsWith('/v1/auth/refresh')) {
        return jsonResponse(200, { accessToken: 'new', refreshToken: 'refresh-1', accessTokenExpiresIn: 3600 })
      }
      const authorization = new Headers(init?.headers).get('Authorization')
      if (authorization === 'Bearer old') {
        return jsonResponse(401, errorBody(401, 'AUTH_FAILED'))
      }
      return jsonResponse(200, [])
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiRequest('/v1/students')).resolves.toEqual([])
    expect(readTokens()?.accessToken).toBe('new')
    expect(fetchMock.mock.calls.filter((call) => String(call[0]).endsWith('/v1/auth/refresh'))).toHaveLength(1)
  })

  it('does not refresh again after the retried request is still unauthorized', async () => {
    saveTokens({ accessToken: 'old', refreshToken: 'refresh-1', accessTokenExpiresIn: 3600 })
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/v1/auth/refresh')) {
        return jsonResponse(200, { accessToken: 'new', refreshToken: 'refresh-1', accessTokenExpiresIn: 3600 })
      }
      return jsonResponse(401, errorBody(401, 'AUTH_FAILED'))
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiRequest('/v1/students')).rejects.toBeInstanceOf(ApiError)
    expect(fetchMock.mock.calls.filter((call) => String(call[0]).endsWith('/v1/auth/refresh'))).toHaveLength(1)
    expect(fetchMock.mock.calls.filter((call) => String(call[0]).endsWith('/v1/students'))).toHaveLength(2)
  })
})

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function errorBody(status: number, code: string) {
  return {
    timestamp: '2026-09-25T15:00:00+09:00',
    status,
    code,
    message: '인증에 실패했습니다.',
    path: '/api/v1/students',
    traceId: 'trace-2',
    fieldErrors: [],
  }
}
