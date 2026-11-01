import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import { StudentListPage } from './StudentListPage.tsx'

vi.mock('../api/studentApi.ts', () => ({
  listStudents: vi.fn(),
  createStudent: vi.fn(),
  updateStudent: vi.fn(),
  releaseStudent: vi.fn(),
  getStudentLearning: vi.fn(),
  getStudentAccess: vi.fn(),
}))

import { createStudent, getStudentAccess, getStudentLearning, listStudents, releaseStudent, updateStudent } from '../api/studentApi.ts'

const listed = {
  studentId: 10,
  email: 'student@lessonpt.test',
  name: '기존학생',
  phone: null,
  teacherStudentId: 20,
}

afterEach(() => {
  cleanup()
})

beforeEach(() => {
  vi.mocked(listStudents).mockReset()
  vi.mocked(createStudent).mockReset()
  vi.mocked(updateStudent).mockReset()
  vi.mocked(releaseStudent).mockReset()
  vi.mocked(getStudentLearning).mockReset()
  vi.mocked(getStudentAccess).mockReset()
  vi.mocked(listStudents).mockResolvedValue([listed])
  vi.mocked(getStudentLearning).mockResolvedValue({
    studentId: 10,
    name: '기존학생',
    email: listed.email,
    phone: null,
    locations: [],
  })
  vi.mocked(getStudentAccess).mockRejectedValue(new ApiError(404, 'COMMON_NOT_FOUND', '요청한 대상을 찾을 수 없습니다.', null, []))
})

it('creates a student and shows the new row without another list request', async () => {
  const user = userEvent.setup()
  vi.mocked(createStudent).mockResolvedValue({
    studentId: 11,
    email: 'new@lessonpt.test',
    name: '새학생',
    phone: '010',
    teacherStudentId: 21,
  })
  renderPage()

  await user.click(await screen.findByRole('button', { name: '학생 추가' }))
  await user.type(screen.getByLabelText('이름'), '새학생')
  await user.type(screen.getByLabelText('전화번호'), '010')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('새학생')).toBeTruthy()
  expect(createStudent).toHaveBeenCalledWith({ name: '새학생', email: null, phone: '010' })
  expect(listStudents).toHaveBeenCalledTimes(1)
})

it('shows an accessible validation message beside the name field', async () => {
  const user = userEvent.setup()
  vi.mocked(createStudent).mockRejectedValue(
    new ApiError(400, 'COMMON_INVALID_INPUT', '요청 값이 올바르지 않습니다.', 'trace-9', [
      { field: 'name', message: '이름을 확인해 주세요.' },
    ]),
  )
  renderPage()

  await user.click(await screen.findByRole('button', { name: '학생 추가' }))
  await user.type(screen.getByLabelText('이름'), '새학생')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('이름을 확인해 주세요.')).toBeTruthy()
  expect(screen.getByText('입력값을 확인해 주세요.')).toBeTruthy()
})

it('updates the listed name after a successful edit', async () => {
  const user = userEvent.setup()
  vi.mocked(updateStudent).mockResolvedValue({ ...listed, name: '수정학생' })
  renderPage()

  await user.click(await screen.findByRole('button', { name: '편집' }))
  const name = screen.getByLabelText('이름')
  await user.clear(name)
  await user.type(name, '수정학생')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('수정학생')).toBeTruthy()
  expect(updateStudent).toHaveBeenCalledWith(10, {
    name: '수정학생',
    email: 'student@lessonpt.test',
    phone: null,
  })
})

it('asks before releasing a student and then removes the row', async () => {
  const user = userEvent.setup()
  vi.mocked(releaseStudent).mockResolvedValue(undefined)
  renderPage()

  await user.click(await screen.findByRole('button', { name: '학생 삭제' }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog.textContent).toContain('학생 기록은 남고')
  expect(releaseStudent).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '학생 삭제' }))

  expect(releaseStudent).toHaveBeenCalledWith(10)
  expect(await screen.findByText('등록된 학생이 없습니다.')).toBeTruthy()
})

it('shows location, curriculum, progress, portal, and a memo indicator on each row', async () => {
  vi.mocked(getStudentLearning).mockResolvedValue({
    studentId: 10,
    name: '기존학생',
    email: listed.email,
    phone: null,
    locations: [
      {
        teacherStudentLocationId: 1,
        locationId: 2,
        locationName: '수원 레슨실',
        address: null,
        studentCurriculums: [
          {
            studentCurriculumId: 7,
            curriculumId: 3,
            curriculumName: 'Drum Basic',
            reenrolled: false,
            memo: '손목 힘이 많이 들어감',
            progress: { completedCount: 8, totalCount: 25, percentage: 32 },
            monitorings: [],
          },
        ],
      },
    ],
  })
  vi.mocked(getStudentAccess).mockResolvedValue({
    teacherStudentAccessId: 4,
    teacherStudentId: 20,
    publicAccessKey: 'key',
    createdAt: null,
    status: 'ACTIVE',
  })
  renderPage()

  expect(await screen.findByText('수원 레슨실')).toBeTruthy()
  const studentLink = screen.getByRole('link', { name: '기존학생' })
  expect(studentLink.getAttribute('href')).toBe('/students/10')
  expect(studentLink.className).toContain('name-link')
  expect(screen.queryByRole('columnheader', { name: '이메일' })).toBeNull()
  expect(screen.getByRole('columnheader', { name: '전화번호' })).toBeTruthy()
  expect(screen.queryByRole('columnheader', { name: '연락' })).toBeNull()
  expect(screen.queryByText('student@lessonpt.test')).toBeNull()
  expect(screen.getByText('Drum Basic')).toBeTruthy()
  expect(screen.getByText('32.0%')).toBeTruthy()
  expect(screen.getByText('32.0%').closest('.progress-meter')).toBeTruthy()
  expect(screen.queryByText('Portal 활성')).toBeNull()
  expect(getStudentAccess).not.toHaveBeenCalled()
  expect(screen.getByLabelText('메모가 있습니다.').getAttribute('title')).toBe('메모가 있습니다.')
})

function renderPage() {
  render(
    <MemoryRouter>
      <StudentListPage />
    </MemoryRouter>,
  )
}

it('filters students and links directly to learning management', async () => {
  const user = userEvent.setup()
  renderPage()
  await screen.findByRole('link', { name: '기존학생' })
  expect(screen.getByRole('link', { name: '학습관리' }).getAttribute('href')).toBe('/students/10?tab=learning')
  await user.type(screen.getByRole('searchbox'), '없는학생')
  expect(screen.queryByRole('link', { name: '기존학생' })).toBeNull()
  expect(screen.getByText('검색 결과가 없습니다.')).toBeTruthy()
})

it('protects unsaved input and keeps the form when discarding is cancelled', async () => {
  const user = userEvent.setup()
  renderPage()
  await user.click(screen.getByRole('button', { name: '학생 추가' }))
  expect(screen.queryByLabelText('이메일')).toBeNull()
  await user.type(screen.getByLabelText('이름'), '작성중')
  await user.keyboard('{Escape}')
  expect(screen.getByRole('dialog', { name: '입력을 버릴까요?' })).toBeTruthy()
  await user.click(screen.getByRole('button', { name: '계속 입력' }))
  expect((screen.getByLabelText('이름') as HTMLInputElement).value).toBe('작성중')
  await user.click(screen.getByRole('button', { name: '취소' }))
  await user.click(screen.getByRole('button', { name: '입력 버리기' }))
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(createStudent).not.toHaveBeenCalled()
})
