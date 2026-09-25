import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { AuthProvider } from './AuthContext.tsx'
import { RequireTeacherAuth } from './RequireTeacherAuth.tsx'
import { clearTokens, saveTokens } from './tokenStore.ts'

afterEach(() => {
  clearTokens()
  vi.unstubAllGlobals()
})

it('sends an anonymous teacher to login', async () => {
  renderGuard(['/students'])
  expect(await screen.findByText('login-page')).toBeTruthy()
})

it('renders the protected page after the teacher session is confirmed', async () => {
  saveTokens({ accessToken: 'access', refreshToken: 'refresh', accessTokenExpiresIn: 3600 })
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      new Response(
        JSON.stringify({ teacherId: 1, email: 'teacher@lessonpt.local', name: '김강사', phone: null }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    ),
  )

  renderGuard(['/students'])
  expect(await screen.findByText('students-page')).toBeTruthy()
})

function renderGuard(initialEntries: string[]) {
  render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<p>login-page</p>} />
          <Route element={<RequireTeacherAuth />}>
            <Route path="/students" element={<p>students-page</p>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}
