import { ApiError } from '../api/apiClient.ts'

export function uploadErrorMessage(error: unknown, kind: 'sheet' | 'audio'): string {
  if (!(error instanceof ApiError)) {
    return '파일을 올리지 못했습니다.'
  }
  if (error.code === 'RESOURCE_INVALID_FILE') {
    return kind === 'sheet' ? 'PDF 형식 오류' : 'Audio 형식 오류'
  }
  if (error.code === 'RESOURCE_FILE_TOO_LARGE') {
    return kind === 'sheet' ? 'PDF 20MB 초과' : 'Audio 50MB 초과'
  }
  if (error.code === 'RESOURCE_STORAGE_LIMIT_EXCEEDED') {
    return '저장 용량 한도에 도달해 파일을 올릴 수 없습니다.'
  }
  if (error.code === 'RESOURCE_STORAGE_CONFLICT') {
    return '다른 업로드가 저장 용량을 확인 중입니다. 잠시 후 다시 시도해 주세요.'
  }
  return '파일을 올리지 못했습니다.'
}
