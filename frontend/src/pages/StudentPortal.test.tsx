import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import { saveTokens } from '../auth/tokenStore.ts'
import { StudentPortalProvider } from '../auth/StudentPortalContext.tsx'
import { StudentAccessPage } from './StudentAccessPage.tsx'
import { StudentLoginPage } from './StudentLoginPage.tsx'
import { StudentPortalPage } from './StudentPortalPage.tsx'
import { StudentRelationshipsPage } from './StudentRelationshipsPage.tsx'
import { StudentVerifyPage } from './StudentVerifyPage.tsx'

vi.mock('../api/studentPortalApi.ts', () => ({
  requestStudentOtp: vi.fn(),
  verifyStudentOtp: vi.fn(),
  requestStudentLoginOtp: vi.fn(),
  verifyStudentLoginOtp: vi.fn(),
  getStudentMe: vi.fn(),
  getStudentLearning: vi.fn(),
  listStudentRelationships: vi.fn(),
  selectStudentScope: vi.fn(),
  clearStudentScope: vi.fn(),
  logoutStudent: vi.fn(),
  isStudentScopeRequired: (error: unknown) =>
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    'code' in error &&
    (error as { status: number }).status === 409 &&
    (error as { code: string }).code === 'STUDENT_SCOPE_REQUIRED',
}))

import {
  clearStudentScope,
  getStudentLearning,
  getStudentMe,
  listStudentRelationships,
  logoutStudent,
  requestStudentLoginOtp,
  requestStudentOtp,
  selectStudentScope,
  verifyStudentLoginOtp,
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
  vi.mocked(requestStudentLoginOtp).mockReset()
  vi.mocked(verifyStudentLoginOtp).mockReset()
  vi.mocked(getStudentMe).mockReset()
  vi.mocked(getStudentLearning).mockReset()
  vi.mocked(listStudentRelationships).mockReset()
  vi.mocked(selectStudentScope).mockReset()
  vi.mocked(clearStudentScope).mockReset()
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

it('sends an unauthenticated student to the student login', async () => {
  vi.mocked(getStudentMe).mockRejectedValue(new ApiError(401, 'AUTH_FAILED', '인증에 실패했습니다.', 'trace-s', []))
  renderPortal('/student')

  expect(await screen.findByRole('heading', { name: '학생 로그인' })).toBeTruthy()
  expect(screen.queryByText('강사 로그인 화면')).toBeNull()
  expect(screen.queryByText('받은 접속 링크를 다시 열어 주세요.')).toBeNull()
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
  expect(await screen.findByRole('heading', { name: '학생 로그인' })).toBeTruthy()
  expect(sessionStorage.getItem('lessonpt.teacher.accessToken')).toBe('teacher-access')
  expect(sessionStorage.getItem('lessonpt.teacher.refreshToken')).toBe('teacher-refresh')
})

const teacherA = {
  teacherStudentAccessId: 11,
  teacherStudentId: 21,
  teacherName: '별돌이',
  locations: [{ locationId: 1, locationName: '하나교회' }],
}
const teacherB = {
  teacherStudentAccessId: 12,
  teacherStudentId: 22,
  teacherName: '박',
  locations: [{ locationId: 2, locationName: '개인레슨실' }],
}
const teacherWithoutLocation = {
  teacherStudentAccessId: 13,
  teacherStudentId: 23,
  teacherName: '최',
  locations: [],
}

function curriculum(name: string) {
  return { curriculums: [{ name, progress: null, categories: [] }] }
}

it('requests a general student otp without storing the email', async () => {
  const user = userEvent.setup()
  const setItem = vi.spyOn(Storage.prototype, 'setItem')
  vi.mocked(requestStudentLoginOtp).mockResolvedValue(undefined)
  renderPortal('/student/login')

  await user.type(await screen.findByLabelText('이메일'), 'student@example.com')
  await user.click(screen.getByRole('button', { name: '인증번호 받기' }))

  expect(requestStudentLoginOtp).toHaveBeenCalledWith('student@example.com')
  expect(requestStudentOtp).not.toHaveBeenCalled()
  expect(await screen.findByRole('heading', { name: '인증번호 확인' })).toBeTruthy()
  expect(setItem).not.toHaveBeenCalled()
  setItem.mockRestore()
})

it('verifies a general otp and opens the relationship list', async () => {
  const user = userEvent.setup()
  vi.mocked(requestStudentLoginOtp).mockResolvedValue(undefined)
  vi.mocked(verifyStudentLoginOtp).mockResolvedValue({ verified: true })
  vi.mocked(getStudentMe).mockResolvedValue({ name: '김학생' })
  vi.mocked(listStudentRelationships).mockResolvedValue([teacherA, teacherB])
  renderPortal('/student/login')

  await user.type(await screen.findByLabelText('이메일'), 'student@example.com')
  await user.click(screen.getByRole('button', { name: '인증번호 받기' }))
  await user.type(await screen.findByLabelText('인증번호'), '123456')
  await user.click(screen.getByRole('button', { name: '확인' }))

  expect(verifyStudentLoginOtp).toHaveBeenCalledWith('student@example.com', '123456')
  expect(verifyStudentOtp).not.toHaveBeenCalled()
  expect(await screen.findByRole('heading', { name: '김학생님, 수업을 선택하세요.' })).toBeTruthy()
  expect(screen.getByRole('heading', { name: '별돌이 선생님' })).toBeTruthy()
  expect(screen.getByText('하나교회')).toBeTruthy()
  expect(screen.getByRole('heading', { name: '박 선생님' })).toBeTruthy()
  expect(screen.getByText('개인레슨실')).toBeTruthy()
  expect(getStudentLearning).not.toHaveBeenCalled()
})

it('shows an empty relationship list', async () => {
  vi.mocked(getStudentMe).mockResolvedValue({ name: '김학생' })
  vi.mocked(listStudentRelationships).mockResolvedValue([])
  renderPortal('/student/relationships')

  expect(await screen.findByText('접근 가능한 수업이 없습니다.')).toBeTruthy()
  expect(selectStudentScope).not.toHaveBeenCalled()
  expect(getStudentLearning).not.toHaveBeenCalled()
})

it('selects the only relationship and opens that portal', async () => {
  vi.mocked(getStudentMe).mockResolvedValue({ name: '김학생' })
  vi.mocked(listStudentRelationships).mockResolvedValue([teacherA])
  vi.mocked(selectStudentScope).mockResolvedValue(undefined)
  vi.mocked(getStudentLearning).mockResolvedValue(curriculum('별돌이수업'))
  renderPortal('/student/relationships')

  expect(await screen.findByRole('heading', { name: '김학생' })).toBeTruthy()
  expect(selectStudentScope).toHaveBeenCalledWith(11)
  expect(selectStudentScope).not.toHaveBeenCalledWith(expect.objectContaining({ teacherStudentId: 21 }))
  expect(screen.getByRole('heading', { name: '별돌이수업' })).toBeTruthy()
  expect(screen.queryByText('박수업')).toBeNull()
})

it('shows location fallback and lets the student pick one of several teachers', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentMe).mockResolvedValue({ name: '김학생' })
  vi.mocked(listStudentRelationships).mockResolvedValue([teacherA, teacherWithoutLocation])
  vi.mocked(selectStudentScope).mockResolvedValue(undefined)
  vi.mocked(getStudentLearning).mockResolvedValue(curriculum('별돌이수업'))
  renderPortal('/student/relationships')

  expect(await screen.findByText('등록된 출강처 없음')).toBeTruthy()
  expect(selectStudentScope).not.toHaveBeenCalled()
  await user.click(screen.getAllByRole('button', { name: '수업 보기' })[0])

  expect(selectStudentScope).toHaveBeenCalledWith(11)
  expect(await screen.findByRole('heading', { name: '별돌이수업' })).toBeTruthy()
})

it('sends a scoped-missing portal to the relationship list', async () => {
  vi.mocked(getStudentMe).mockResolvedValue({ name: '김학생' })
  vi.mocked(getStudentLearning).mockRejectedValue(
    new ApiError(409, 'STUDENT_SCOPE_REQUIRED', '수업 범위를 먼저 선택해 주세요.', 'trace-s', []),
  )
  vi.mocked(listStudentRelationships).mockResolvedValue([teacherA, teacherB])
  renderPortal('/student')

  expect(await screen.findByRole('heading', { name: '김학생님, 수업을 선택하세요.' })).toBeTruthy()
  expect(screen.queryByText('수업 범위를 먼저 선택해 주세요.')).toBeNull()
  expect(screen.queryByText('현재 상태에서는 실행할 수 없습니다.')).toBeNull()
})

it('keeps one teacher curriculum off the screen after switching to the other', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentMe).mockResolvedValue({ name: '김학생' })
  vi.mocked(listStudentRelationships).mockResolvedValue([teacherA, teacherB])
  vi.mocked(selectStudentScope).mockResolvedValue(undefined)
  vi.mocked(clearStudentScope).mockResolvedValue(undefined)
  vi.mocked(getStudentLearning)
    .mockResolvedValueOnce(curriculum('별돌이수업'))
    .mockResolvedValueOnce(curriculum('박수업'))
  renderPortal('/student/relationships')

  await user.click((await screen.findAllByRole('button', { name: '수업 보기' }))[0])
  expect(await screen.findByRole('heading', { name: '별돌이수업' })).toBeTruthy()
  expect(screen.queryByRole('heading', { name: '박수업' })).toBeNull()

  await user.click(screen.getByRole('button', { name: '다른 수업 보기' }))
  expect(clearStudentScope).toHaveBeenCalledTimes(1)
  expect(await screen.findByRole('heading', { name: '김학생님, 수업을 선택하세요.' })).toBeTruthy()

  await user.click(screen.getAllByRole('button', { name: '수업 보기' })[1])
  expect(selectStudentScope).toHaveBeenLastCalledWith(12)
  expect(await screen.findByRole('heading', { name: '박수업' })).toBeTruthy()
  expect(screen.queryByRole('heading', { name: '별돌이수업' })).toBeNull()
})

it('opens the portal directly after an invitation verify', async () => {
  const user = userEvent.setup()
  vi.mocked(requestStudentOtp).mockResolvedValue(undefined)
  vi.mocked(verifyStudentOtp).mockResolvedValue({ verified: true })
  vi.mocked(getStudentMe).mockResolvedValue({ name: '학생' })
  vi.mocked(getStudentLearning).mockResolvedValue(curriculum('초대수업'))
  renderPortal('/student/access/access-key')

  await user.type(await screen.findByLabelText('이메일'), 'student@example.com')
  await user.click(screen.getByRole('button', { name: '인증번호 받기' }))
  await user.type(await screen.findByLabelText('인증번호'), '123456')
  await user.click(screen.getByRole('button', { name: '확인' }))

  expect(verifyStudentOtp).toHaveBeenCalledWith('access-key', 'student@example.com', '123456')
  expect(verifyStudentLoginOtp).not.toHaveBeenCalled()
  expect(listStudentRelationships).not.toHaveBeenCalled()
  expect(await screen.findByRole('heading', { name: '초대수업' })).toBeTruthy()
})

it('links the invitation fallback to general student login', async () => {
  renderPortal('/student/access')

  expect(await screen.findByText('받은 접속 링크를 다시 열어 주세요.')).toBeTruthy()
  expect(screen.getByRole('link', { name: '일반 학생 로그인' }).getAttribute('href')).toBe('/student/login')
})

function renderPortal(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <StudentPortalProvider>
        <Routes>
          <Route path="/login" element={<p>강사 로그인 화면</p>} />
          <Route path="/student/login" element={<StudentLoginPage />} />
          <Route path="/student/access" element={<StudentAccessPage />} />
          <Route path="/student/access/:publicAccessKey" element={<StudentAccessPage />} />
          <Route path="/student/verify" element={<StudentVerifyPage />} />
          <Route path="/student/relationships" element={<StudentRelationshipsPage />} />
          <Route path="/student" element={<StudentPortalPage />} />
        </Routes>
      </StudentPortalProvider>
    </MemoryRouter>,
  )
}
