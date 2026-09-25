import { ApiError } from '../../api/apiClient.ts'

export type ErrorView = {
  title: string
  detail: string
  traceId: string | null
}

export function describeError(error: unknown): ErrorView {
  if (!(error instanceof ApiError)) {
    return { title: '오류', detail: '요청을 처리하지 못했습니다.', traceId: null }
  }
  if (error.status === 400) {
    const fields = error.fieldErrors.map((item) => item.message).filter(Boolean)
    return {
      title: '입력값 오류',
      detail: fields.length > 0 ? fields.join(' ') : '입력값을 확인해 주세요.',
      traceId: error.traceId,
    }
  }
  if (error.status === 401) {
    return { title: '인증 만료', detail: '다시 로그인해 주세요.', traceId: error.traceId }
  }
  if (error.status === 404) {
    return { title: '대상 없음', detail: '요청한 대상을 찾을 수 없습니다.', traceId: error.traceId }
  }
  if (error.status === 409) {
    return { title: '실행할 수 없음', detail: '현재 상태에서는 실행할 수 없습니다.', traceId: error.traceId }
  }
  return { title: '서버 오류', detail: '잠시 후 다시 시도해 주세요.', traceId: error.traceId }
}

export function fieldErrorMessage(error: unknown, field: string): string | null {
  if (!(error instanceof ApiError)) {
    return null
  }
  return error.fieldErrors.find((item) => item.field === field)?.message ?? null
}

export function formErrorMessage(error: unknown): string | null {
  if (!(error instanceof ApiError)) {
    return '요청을 처리하지 못했습니다.'
  }
  if (error.fieldErrors.length > 0) {
    return '입력값을 확인해 주세요.'
  }
  return describeError(error).detail
}
