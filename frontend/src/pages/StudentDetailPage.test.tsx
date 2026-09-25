import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import type { StudentLearningDetail } from '../types/student.ts'
import { StudentDetailPage } from './StudentDetailPage.tsx'

vi.mock('../api/studentApi.ts', () => ({
  getStudentLearning: vi.fn(),
}))

vi.mock('../api/locationApi.ts', () => ({
  listLocations: vi.fn(),
  assignStudentLocation: vi.fn(),
  releaseStudentLocation: vi.fn(),
}))

import { assignStudentLocation, listLocations, releaseStudentLocation } from '../api/locationApi.ts'
import { getStudentLearning } from '../api/studentApi.ts'

const catalog = [
  {
    locationId: 30,
    name: '연습실',
    displayOrder: 1,
    address: '서울',
    createdAt: '2026-09-25T00:00:00',
    updatedAt: '2026-09-25T00:00:00',
  },
]

const emptyLearning: StudentLearningDetail = {
  studentId: 41,
  name: '기존학생',
  email: null,
  phone: null,
  locations: [],
}

const assignedLearning: StudentLearningDetail = {
  ...emptyLearning,
  locations: [
    {
      teacherStudentLocationId: 90,
      locationId: 30,
      locationName: '연습실',
      address: '서울',
      studentCurriculums: [
        {
          studentCurriculumId: 7,
          curriculumId: 3,
          curriculumName: '기초',
          reenrolled: false,
          memo: null,
          progress: null,
          monitorings: [],
        },
      ],
    },
  ],
}

const reassignedLearning: StudentLearningDetail = {
  ...emptyLearning,
  locations: [
    {
      teacherStudentLocationId: 90,
      locationId: 30,
      locationName: '연습실',
      address: '서울',
      studentCurriculums: [],
    },
  ],
}

afterEach(() => {
  cleanup()
})

beforeEach(() => {
  vi.mocked(getStudentLearning).mockReset()
  vi.mocked(listLocations).mockReset()
  vi.mocked(assignStudentLocation).mockReset()
  vi.mocked(releaseStudentLocation).mockReset()
  vi.mocked(listLocations).mockResolvedValue(catalog)
})

it('assigns a location and refetches the learning detail', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentLearning).mockResolvedValueOnce(emptyLearning).mockResolvedValueOnce(reassignedLearning)
  vi.mocked(assignStudentLocation).mockResolvedValue({
    teacherStudentLocationId: 90,
    locationId: 30,
    locationName: '연습실',
    address: '서울',
  })
  renderPage()

  await user.selectOptions(await screen.findByLabelText('배정할 출강처'), '30')
  await user.click(screen.getByRole('button', { name: '배정' }))

  expect(assignStudentLocation).toHaveBeenCalledWith(41, 30)
  expect(await screen.findByText('출강처를 배정했습니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
  await user.click(screen.getByRole('tab', { name: '학습관리' }))
  expect(await screen.findByText('배정된 커리큘럼이 없습니다.')).toBeTruthy()
})

it('asks before releasing a location and then refetches learning', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentLearning).mockResolvedValueOnce(assignedLearning).mockResolvedValueOnce(emptyLearning)
  vi.mocked(releaseStudentLocation).mockResolvedValue(undefined)
  renderPage()

  await user.click(await screen.findByRole('button', { name: '해제' }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog.textContent).toContain('활성 수강')
  expect(dialog.textContent).toContain('자동으로 돌아오지 않습니다')
  expect(releaseStudentLocation).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '해제' }))

  expect(releaseStudentLocation).toHaveBeenCalledWith(41, 30)
  expect(await screen.findByText('배정된 출강처가 없습니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
})

it('shows a conflict when the location is already assigned and refreshes learning', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentLearning).mockResolvedValueOnce(emptyLearning).mockResolvedValueOnce(assignedLearning)
  vi.mocked(assignStudentLocation).mockRejectedValue(
    new ApiError(409, 'COMMON_CONFLICT', '요청이 현재 상태와 충돌합니다.', 'trace-4', []),
  )
  renderPage()

  await user.selectOptions(await screen.findByLabelText('배정할 출강처'), '30')
  await user.click(screen.getByRole('button', { name: '배정' }))

  expect(await screen.findByText('이미 배정된 출강처입니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
  expect(screen.queryByLabelText('배정할 출강처')).toBeNull()
  await user.click(screen.getByRole('tab', { name: '학습관리' }))
  expect(await screen.findByText('기초')).toBeTruthy()
})

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/students/41']}>
      <Routes>
        <Route path="/students/:studentId" element={<StudentDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}
