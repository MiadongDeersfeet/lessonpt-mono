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
}))

import { createStudent, listStudents, releaseStudent, updateStudent } from '../api/studentApi.ts'

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
  vi.mocked(listStudents).mockResolvedValue([listed])
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
  await user.type(screen.getByLabelText('이메일'), 'new@lessonpt.test')
  await user.type(screen.getByLabelText('전화번호'), '010')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('새학생')).toBeTruthy()
  expect(createStudent).toHaveBeenCalledWith({ name: '새학생', email: 'new@lessonpt.test', phone: '010' })
  expect(listStudents).toHaveBeenCalledTimes(1)
})

it('shows a validation message beside the email field', async () => {
  const user = userEvent.setup()
  vi.mocked(createStudent).mockRejectedValue(
    new ApiError(400, 'COMMON_INVALID_INPUT', '요청 값이 올바르지 않습니다.', 'trace-9', [
      { field: 'email', message: '이메일 형식이 올바르지 않습니다.' },
    ]),
  )
  renderPage()

  await user.click(await screen.findByRole('button', { name: '학생 추가' }))
  await user.type(screen.getByLabelText('이름'), '새학생')
  await user.type(screen.getByLabelText('이메일'), 'not-an-email')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('이메일 형식이 올바르지 않습니다.')).toBeTruthy()
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

  await user.click(await screen.findByRole('button', { name: '연결 해제' }))
  const dialog = await screen.findByRole('dialog')
  expect(releaseStudent).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '연결 해제' }))

  expect(releaseStudent).toHaveBeenCalledWith(10)
  expect(await screen.findByText('등록된 학생이 없습니다.')).toBeTruthy()
})

function renderPage() {
  render(
    <MemoryRouter>
      <StudentListPage />
    </MemoryRouter>,
  )
}
