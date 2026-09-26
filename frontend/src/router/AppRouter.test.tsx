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

it('keeps teacher students, locations, and curriculums behind teacher auth', async () => {
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

  expect(await screen.findByRole('heading', { name: '학생' })).toBeTruthy()
  expect(screen.queryByRole('link', { name: '대시보드' })).toBeNull()
  const students = screen.getByRole('link', { name: '학생' })
  expect(students.getAttribute('href')).toBe('/students')
  expect(students.getAttribute('aria-current')).toBe('page')
  expect(students.className).toContain('active')
  expect(screen.getByRole('link', { name: '출강처' }).getAttribute('aria-current')).toBeNull()
  expect(sessionStorage.getItem('lessonpt.teacher.accessToken')).toBe('teacher-access')
  await user.click(screen.getByRole('link', { name: '출강처' }))
  expect(await screen.findByRole('heading', { name: '출강처' })).toBeTruthy()
  expect(screen.getByRole('link', { name: '출강처' }).getAttribute('aria-current')).toBe('page')
  expect(screen.getByRole('link', { name: '학생' }).getAttribute('aria-current')).toBeNull()
  expect(sessionStorage.getItem('lessonpt.teacher.accessToken')).toBe('teacher-access')
  await user.click(screen.getByRole('link', { name: '커리큘럼' }))
  expect(await screen.findByRole('heading', { name: '커리큘럼' })).toBeTruthy()
})

it('sends an anonymous teacher away from the dashboard', async () => {
  renderRouter('/dashboard')
  expect(await screen.findByRole('heading', { name: '강사 로그인' })).toBeTruthy()
  expect(screen.queryByRole('heading', { name: '대시보드' })).toBeNull()
})

it('shows a not found page for an unknown route and returns home', async () => {
  const user = userEvent.setup()
  renderRouter('/this-route-does-not-exist')

  expect(await screen.findByRole('heading', { name: '페이지를 찾을 수 없습니다.' })).toBeTruthy()
  expect(screen.getByText('요청하신 페이지가 존재하지 않거나 이동되었습니다.')).toBeTruthy()
  const home = screen.getByRole('link', { name: '홈으로 이동' })
  expect(home.getAttribute('href')).toBe('/')
  await user.click(home)
  expect(await screen.findByRole('heading', { name: 'LessonPT' })).toBeTruthy()
})

it('keeps an unknown student id on the existing student route', async () => {
  renderRouter('/students/not-real')
  expect(await screen.findByRole('heading', { name: '강사 로그인' })).toBeTruthy()
  expect(screen.queryByRole('heading', { name: '페이지를 찾을 수 없습니다.' })).toBeNull()
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
