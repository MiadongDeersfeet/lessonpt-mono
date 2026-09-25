import { expect, it } from 'vitest'
import { ApiError } from '../../api/apiClient.ts'
import { fieldErrorMessage, formErrorMessage } from './describeError.ts'

it('places fieldErrors on the matching field and keeps a form-level prompt', () => {
  const error = new ApiError(400, 'COMMON_INVALID_INPUT', '요청 값이 올바르지 않습니다.', 'trace-1', [
    { field: 'email', message: '이메일 형식이 올바르지 않습니다.' },
  ])

  expect(fieldErrorMessage(error, 'email')).toBe('이메일 형식이 올바르지 않습니다.')
  expect(fieldErrorMessage(error, 'name')).toBeNull()
  expect(formErrorMessage(error)).toBe('입력값을 확인해 주세요.')
})

it('shows the location order conflict message from the server', () => {
  const message = '다른 요청이 장소 순서를 변경 중입니다. 잠시 후 다시 시도해 주세요.'
  const error = new ApiError(409, 'ORDER_CONFLICT', message, 'trace-2', [])

  expect(formErrorMessage(error)).toBe(message)
})
