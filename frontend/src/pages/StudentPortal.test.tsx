import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import { saveTokens } from '../auth/tokenStore.ts'
import { StudentPortalProvider } from '../auth/StudentPortalContext.tsx'
import { StudentAccessPage } from './StudentAccessPage.tsx'
import { StudentPortalPage } from './StudentPortalPage.tsx'
import { StudentVerifyPage } from './StudentVerifyPage.tsx'

vi.mock('../api/studentPortalApi.ts', () => ({
  requestStudentOtp: vi.fn(),
  verifyStudentOtp: vi.fn(),
  getStudentMe: vi.fn(),
  getStudentLearning: vi.fn(),
  logoutStudent: vi.fn(),
}))

import {
  getStudentLearning,
  getStudentMe,
  logoutStudent,
  requestStudentOtp,
  verifyStudentOtp,
} from '../api/studentPortalApi.ts'

const learning = {
  curriculums: [
    {
      name: '기초',
      progress: null,
      categories: [
        {
          name: '루디먼트',
          contents: [
            {
              name: '싱글',
              targetBpm: 80,
              currentBpm: null,
              progressStatus: 'YET' as const,
              sheetUrl: null,
              youtubeUrl: null,
              audioUrl: null,
              homeworks: [{ content: '메트로놈', deadline: null, completed: true, feedback: null }],
            },
            {
              name: '더블',
              targetBpm: null,
              currentBpm: 90,
              progressStatus: 'IN_PROGRESS' as const,
              sheetUrl: null,
              youtubeUrl: null,
              audioUrl: null,
              homeworks: [],
            },
            {
              name: '파라디들',
              targetBpm: null,
              currentBpm: null,
              progressStatus: 'COMPLETED' as const,
              sheetUrl: null,
              youtubeUrl: null,
              audioUrl: null,
              homeworks: [],
            },
            {
              name: '플램',
              targetBpm: null,
              currentBpm: null,
              progressStatus: 'STOPPED' as const,
              sheetUrl: null,
              youtubeUrl: null,
              audioUrl: null,
              homeworks: [],
            },
          ],
        },
      ],
    },
    {
      name: '응용',
      progress: { completedCount: 1, totalCount: 2, percentage: 50 },
      categories: [],
    },
  ],
}

afterEach(() => {
  cleanup()
  sessionStorage.clear()
  localStorage.clear()
})

beforeEach(() => {
  vi.mocked(requestStudentOtp).mockReset()
  vi.mocked(verifyStudentOtp).mockReset()
  vi.mocked(getStudentMe).mockReset()
  vi.mocked(getStudentLearning).mockReset()
  vi.mocked(logoutStudent).mockReset()
})

it('requests an otp from the public access link without storing the code', async () => {
  const user = userEvent.setup()
  const setItem = vi.spyOn(Storage.prototype, 'setItem')
  vi.mocked(requestStudentOtp).mockResolvedValue(undefined)
  renderPortal('/student/access/access-key')

  expect(await screen.findByRole('heading', { name: '학생 접속' })).toBeTruthy()
  await user.type(screen.getByLabelText('이메일'), 'student@example.com')
  await user.click(screen.getByRole('button', { name: '인증번호 받기' }))

  expect(requestStudentOtp).toHaveBeenCalledWith('access-key', 'student@example.com')
  expect(await screen.findByRole('heading', { name: '인증번호 확인' })).toBeTruthy()
  expect(setItem).not.toHaveBeenCalled()
  setItem.mockRestore()
})

it('verifies the otp and loads me and learning from the cookie session', async () => {
  const user = userEvent.setup()
  vi.mocked(requestStudentOtp).mockResolvedValue(undefined)
  vi.mocked(verifyStudentOtp).mockResolvedValue({ verified: true })
  vi.mocked(getStudentMe).mockResolvedValue({ name: '학생' })
  vi.mocked(getStudentLearning).mockResolvedValue(learning)
  renderPortal('/student/access/access-key')

  await user.type(await screen.findByLabelText('이메일'), 'student@example.com')
  await user.click(screen.getByRole('button', { name: '인증번호 받기' }))
  await user.type(await screen.findByLabelText('인증번호'), '123456')
  await user.click(screen.getByRole('button', { name: '확인' }))

  expect(verifyStudentOtp).toHaveBeenCalledWith('access-key', 'student@example.com', '123456')
  expect(await screen.findByRole('heading', { name: '학생' })).toBeTruthy()
  expect(getStudentMe).toHaveBeenCalledTimes(1)
  expect(getStudentLearning).toHaveBeenCalledTimes(1)
  expect(screen.getByText('진행률 계산 대상 없음')).toBeTruthy()
  expect(screen.getByText('50.0%')).toBeTruthy()
  const single = screen.getByRole('heading', { name: '싱글' }).parentElement
  expect(single?.textContent).toContain('시작 전')
  expect(single?.textContent).toContain('현재 BPM -')
  expect(single?.textContent).toContain('메트로놈')
  expect(single?.textContent).toContain('완료')
  expect(single?.textContent).toContain('마감 -')
  expect(single?.textContent).toContain('피드백 -')
  expect(screen.getByRole('heading', { name: '더블' }).parentElement?.textContent).toContain('진행 중')
  expect(screen.getByRole('heading', { name: '파라디들' }).parentElement?.textContent).toContain('완료')
  expect(screen.getByRole('heading', { name: '플램' }).parentElement?.textContent).toContain('중단')
  expect(screen.queryByRole('button', { name: '기록 수정' })).toBeNull()
  expect(screen.queryByRole('button', { name: '과제 추가' })).toBeNull()
  expect(screen.queryByRole('button', { name: '비활성화' })).toBeNull()
  expect(screen.queryByRole('button', { name: '배정' })).toBeNull()
})

it('sends an unauthenticated student back to the access screen', async () => {
  vi.mocked(getStudentMe).mockRejectedValue(new ApiError(401, 'AUTH_FAILED', '인증에 실패했습니다.', 'trace-s', []))
  renderPortal('/student')

  expect(await screen.findByText('받은 접속 링크를 다시 열어 주세요.')).toBeTruthy()
  expect(screen.queryByText('강사 로그인 화면')).toBeNull()
})

it('logs out without clearing the teacher session and blocks the portal', async () => {
  const user = userEvent.setup()
  saveTokens({ accessToken: 'teacher-access', refreshToken: 'teacher-refresh', accessTokenExpiresIn: 60 })
  vi.mocked(requestStudentOtp).mockResolvedValue(undefined)
  vi.mocked(verifyStudentOtp).mockResolvedValue({ verified: true })
  vi.mocked(getStudentMe).mockResolvedValue({ name: '학생' })
  vi.mocked(getStudentLearning).mockResolvedValue({ curriculums: [] })
  vi.mocked(logoutStudent).mockResolvedValue(undefined)
  renderPortal('/student/access/access-key')

  await user.type(await screen.findByLabelText('이메일'), 'student@example.com')
  await user.click(screen.getByRole('button', { name: '인증번호 받기' }))
  await user.type(await screen.findByLabelText('인증번호'), '123456')
  await user.click(screen.getByRole('button', { name: '확인' }))
  await user.click(await screen.findByRole('button', { name: '로그아웃' }))

  expect(logoutStudent).toHaveBeenCalledTimes(1)
  expect(await screen.findByRole('heading', { name: '학생 접속' })).toBeTruthy()
  expect(sessionStorage.getItem('lessonpt.teacher.accessToken')).toBe('teacher-access')
  expect(sessionStorage.getItem('lessonpt.teacher.refreshToken')).toBe('teacher-refresh')
})

function renderPortal(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <StudentPortalProvider>
        <Routes>
          <Route path="/login" element={<p>강사 로그인 화면</p>} />
          <Route path="/student/access" element={<StudentAccessPage />} />
          <Route path="/student/access/:publicAccessKey" element={<StudentAccessPage />} />
          <Route path="/student/verify" element={<StudentVerifyPage />} />
          <Route path="/student" element={<StudentPortalPage />} />
        </Routes>
      </StudentPortalProvider>
    </MemoryRouter>,
  )
}
