const TEACHER_PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$/

export function teacherPasswordMessage(password: string): string | null {
  if (password.length < 8 || password.length > 20) {
    return '비밀번호는 8자 이상 20자 이하여야 합니다.'
  }
  if (!TEACHER_PASSWORD_PATTERN.test(password)) {
    return '비밀번호는 영문자, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.'
  }
  return null
}
