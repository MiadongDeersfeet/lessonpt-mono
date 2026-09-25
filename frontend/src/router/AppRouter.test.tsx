import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { saveTokens } from '../auth/tokenStore.ts'
import { StudentPortalProvider } from '../auth/StudentPortalContext.tsx'
import { AppRouter } from './AppRouter.tsx'

afterEach(() => {
  cleanup()
  sessionStorage.clear()
  vi.unstubAllGlobals()
})

it('sends the landing choices to teacher login and student login', async () => {
  const user = userEvent.setup()
  renderRouter('/')

  await user.click(screen.getByRole('link', { name: /선생님으로 시작하기/ }))
  expect(await screen.findByRole('heading', { name: '강사 로그인' })).toBeTruthy()

  cleanup()
  const nextUser = userEvent.setup()
  renderRouter('/')
  await nextUser.click(screen.getByRole('link', { name: /학생으로 시작하기/ }))
  expect(await screen.findByRole('heading', { name: '학생 로그인' })).toBeTruthy()
})

it('does not call the teacher profile from a student route', async () => {
  saveTokens({ accessToken: 'teacher-access', refreshToken: 'teacher-refresh', accessTokenExpiresIn: 60 })
  const fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  renderRouter('/student/login')

  expect(await screen.findByRole('heading', { name: '학생 로그인' })).toBeTruthy()
  expect(fetchMock).not.toHaveBeenCalled()
  expect(sessionStorage.getItem('lessonpt.teacher.accessToken')).toBe('teacher-access')
})

it('keeps teacher dashboard, students, locations, and curriculums behind teacher auth', async () => {
  const user = userEvent.setup()
  saveTokens({ accessToken: 'teacher-access', refreshToken: 'teacher-refresh', accessTokenExpiresIn: 60 })
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo) => {
      const url = String(input)
      if (url.endsWith('/v1/teachers/me')) {
        return json({ teacherId: 1, email: 'teacher@lessonpt.local', name: '김강사', phone: null })
      }
      return json([])
    }),
  )
  renderRouter('/dashboard')

  expect(await screen.findByRole('heading', { name: '대시보드' })).toBeTruthy()
  expect(screen.getByRole('link', { name: '대시보드' }).getAttribute('href')).toBe('/dashboard')

  await user.click(screen.getByRole('link', { name: '학생' }))
  expect(await screen.findByRole('heading', { name: '학생' })).toBeTruthy()
  await user.click(screen.getByRole('link', { name: '출강처' }))
  expect(await screen.findByRole('heading', { name: '출강처' })).toBeTruthy()
  await user.click(screen.getByRole('link', { name: '커리큘럼' }))
  expect(await screen.findByRole('heading', { name: '커리큘럼' })).toBeTruthy()
})

it('sends an anonymous teacher away from the dashboard', async () => {
  renderRouter('/dashboard')
  expect(await screen.findByRole('heading', { name: '강사 로그인' })).toBeTruthy()
  expect(screen.queryByRole('heading', { name: '대시보드' })).toBeNull()
})

function renderRouter(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <StudentPortalProvider>
        <AppRouter />
      </StudentPortalProvider>
    </MemoryRouter>,
  )
}

function json(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}
