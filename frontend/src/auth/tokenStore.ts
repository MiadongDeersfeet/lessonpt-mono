import type { AuthTokenResponse } from '../types/auth.ts'

const ACCESS_KEY = 'lessonpt.teacher.accessToken'
const REFRESH_KEY = 'lessonpt.teacher.refreshToken'
const EXPIRES_KEY = 'lessonpt.teacher.accessTokenExpiresIn'

/**
 * Teacher 토큰은 JSON 본문으로만 내려온다. HttpOnly 쿠키로 바꿀 수 있는 Backend 계약이 아니다.
 * localStorage는 브라우저를 닫아도 refresh token(14일)이 남는다.
 * sessionStorage는 같은 탭 새로고침은 유지하고, 탭을 닫으면 지운다.
 */
export function readTokens(): AuthTokenResponse | null {
  const accessToken = sessionStorage.getItem(ACCESS_KEY)
  const refreshToken = sessionStorage.getItem(REFRESH_KEY)
  const expiresIn = sessionStorage.getItem(EXPIRES_KEY)
  if (!accessToken || !refreshToken || expiresIn == null) {
    return null
  }
  const accessTokenExpiresIn = Number(expiresIn)
  if (!Number.isFinite(accessTokenExpiresIn)) {
    return null
  }
  return { accessToken, refreshToken, accessTokenExpiresIn }
}

export function saveTokens(tokens: AuthTokenResponse): void {
  sessionStorage.setItem(ACCESS_KEY, tokens.accessToken)
  sessionStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  sessionStorage.setItem(EXPIRES_KEY, String(tokens.accessTokenExpiresIn))
}

export function clearTokens(): void {
  sessionStorage.removeItem(ACCESS_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(EXPIRES_KEY)
}
