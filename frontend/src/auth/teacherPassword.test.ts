import { expect, it } from 'vitest'
import { teacherPasswordMessage } from './teacherPassword.ts'

it('accepts a password with letter, digit, and special character', () => {
  expect(teacherPasswordMessage('Abcdef1!')).toBeNull()
})

it('rejects a password shorter than eight characters', () => {
  expect(teacherPasswordMessage('Abcde1!')).toBe('비밀번호는 8자 이상 20자 이하여야 합니다.')
})

it('rejects a password without a special character', () => {
  expect(teacherPasswordMessage('Abcdefg1')).toBe(
    '비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.',
  )
})
