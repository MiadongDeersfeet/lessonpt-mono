import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import { AuthProvider } from '../auth/AuthContext.tsx'
import { SignupPage } from './SignupPage.tsx'

vi.mock('../api/authApi.ts', () => ({
  signup: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}))

import { login, me, signup } from '../api/authApi.ts'

afterEach(() => {
  cleanup()
  sessionStorage.clear()
})

beforeEach(() => {
  vi.mocked(signup).mockReset()
  vi.mocked(login).mockReset()
  vi.mocked(me).mockReset()
})

it('creates an account and continues into the teacher session', async () => {
  const user = userEvent.setup()
  vi.mocked(signup).mockResolvedValue({
    teacherId: 4,
    email: 'new@lessonpt.local',
    name: '새강사',
    phone: null,
    role: 'TEACHER',
  })
  vi.mocked(login).mockResolvedValue({
    accessToken: 'access',
    refreshToken: 'refresh',
    accessTokenExpiresIn: 3600,
  })
  vi.mocked(me).mockResolvedValue({
    teacherId: 4,
    email: 'new@lessonpt.local',
    name: '새강사',
    phone: null,
  })
  renderPage()

  await user.type(screen.getByLabelText('이메일'), 'new@lessonpt.local')
  await user.type(screen.getByLabelText('이름'), '새강사')
  await user.type(screen.getByLabelText('비밀번호'), 'Abcdef1!')
  await user.type(screen.getByLabelText('비밀번호 확인'), 'Abcdef1!')
  await user.click(screen.getByRole('button', { name: '회원가입' }))

  expect(signup).toHaveBeenCalledWith({ email: 'new@lessonpt.local', password: 'Abcdef1!', name: '새강사' })
  expect(login).toHaveBeenCalledWith({ email: 'new@lessonpt.local', password: 'Abcdef1!' })
  expect(await screen.findByText('students-page')).toBeTruthy()
})

it('keeps the form when the email is already in use', async () => {
  const user = userEvent.setup()
  vi.mocked(signup).mockRejectedValue(
    new ApiError(409, 'COMMON_CONFLICT', '이미 사용 중인 이메일입니다.', 'trace-1', []),
  )
  renderPage()

  await user.type(screen.getByLabelText('이메일'), 'used@lessonpt.local')
  await user.type(screen.getByLabelText('이름'), '새강사')
  await user.type(screen.getByLabelText('비밀번호'), 'Abcdef1!')
  await user.type(screen.getByLabelText('비밀번호 확인'), 'Abcdef1!')
  await user.click(screen.getByRole('button', { name: '회원가입' }))

  expect(await screen.findByText('이미 사용 중인 이메일입니다.')).toBeTruthy()
  expect(login).not.toHaveBeenCalled()
  expect(screen.getByRole('heading', { name: '강사 회원가입' })).toBeTruthy()
})

it('asks for matching passwords before calling signup', async () => {
  const user = userEvent.setup()
  renderPage()

  await user.type(screen.getByLabelText('이메일'), 'new@lessonpt.local')
  await user.type(screen.getByLabelText('이름'), '새강사')
  await user.type(screen.getByLabelText('비밀번호'), 'Abcdef1!')
  await user.type(screen.getByLabelText('비밀번호 확인'), 'Abcdef1?')
  await user.click(screen.getByRole('button', { name: '회원가입' }))

  expect(await screen.findByText('비밀번호 확인이 일치하지 않습니다.')).toBeTruthy()
  expect(signup).not.toHaveBeenCalled()
})

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/signup']}>
      <AuthProvider>
        <Routes>
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/login" element={<p>login-page</p>} />
          <Route path="/students" element={<p>students-page</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}
