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

vi.mock('../api/curriculumApi.ts', () => ({
  listCurriculums: vi.fn(),
}))

vi.mock('../api/studentCurriculumApi.ts', () => ({
  assignStudentCurriculum: vi.fn(),
  updateStudentCurriculumMemo: vi.fn(),
  releaseStudentCurriculum: vi.fn(),
}))

import { assignStudentLocation, listLocations, releaseStudentLocation } from '../api/locationApi.ts'
import { listCurriculums } from '../api/curriculumApi.ts'
import {
  assignStudentCurriculum,
  releaseStudentCurriculum,
  updateStudentCurriculumMemo,
} from '../api/studentCurriculumApi.ts'
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
  vi.mocked(listCurriculums).mockReset()
  vi.mocked(assignStudentCurriculum).mockReset()
  vi.mocked(releaseStudentCurriculum).mockReset()
  vi.mocked(updateStudentCurriculumMemo).mockReset()
  vi.mocked(listCurriculums).mockResolvedValue([
    { curriculumId: 3, name: '기초', displayOrder: 1 },
    { curriculumId: 4, name: '응용', displayOrder: 2 },
  ])
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

const curriculumCatalog = [
  { curriculumId: 3, name: '기초', displayOrder: 1 },
  { curriculumId: 4, name: '응용', displayOrder: 2 },
]

const studioOnly = {
  teacherStudentLocationId: 90,
  locationId: 30,
  locationName: '연습실',
  address: '서울',
  studentCurriculums: [] as StudentLearningDetail['locations'][number]['studentCurriculums'],
}

it('loads the curriculum catalog and hides curricula already assigned at that location', async () => {
  vi.mocked(getStudentLearning).mockResolvedValue(assignedLearning)
  renderPage()

  expect(await screen.findByText('기초')).toBeTruthy()
  expect(listCurriculums).toHaveBeenCalledTimes(1)
  const select = await screen.findByLabelText('배정할 커리큘럼 (연습실)')
  expect(within(select).queryByRole('option', { name: '기초' })).toBeNull()
  expect(within(select).getByRole('option', { name: '응용' })).toBeTruthy()
  expect(screen.queryByRole('button', { name: '복구' })).toBeNull()
  expect(screen.queryByRole('button', { name: '학습 기록 추가' })).toBeNull()
})

it('assigns a curriculum and refetches learning', async () => {
  const user = userEvent.setup()
  vi.mocked(listCurriculums).mockResolvedValue(curriculumCatalog)
  vi.mocked(getStudentLearning).mockResolvedValueOnce({ ...emptyLearning, locations: [studioOnly] }).mockResolvedValueOnce(assignedLearning)
  vi.mocked(assignStudentCurriculum).mockResolvedValue({
    studentCurriculumId: 7,
    curriculumId: 3,
    reenrolled: false,
    memo: '노트',
  })
  renderPage()

  await user.selectOptions(await screen.findByLabelText('배정할 커리큘럼 (연습실)'), '3')
  await user.type(screen.getByLabelText('배정 메모'), '노트')
  await user.click(screen.getByRole('button', { name: '커리큘럼 배정' }))

  expect(assignStudentCurriculum).toHaveBeenCalledWith(90, { curriculumId: 3, memo: '노트' })
  expect(await screen.findByText('커리큘럼을 배정했습니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
})

it('shows a conflict when the curriculum is already assigned at the location', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentLearning)
    .mockResolvedValueOnce({ ...emptyLearning, locations: [studioOnly] })
    .mockResolvedValueOnce(assignedLearning)
  vi.mocked(assignStudentCurriculum).mockRejectedValue(
    new ApiError(409, 'COMMON_CONFLICT', '요청이 현재 상태와 충돌합니다.', 'trace-5', []),
  )
  renderPage()

  await user.selectOptions(await screen.findByLabelText('배정할 커리큘럼 (연습실)'), '3')
  await user.click(screen.getByRole('button', { name: '커리큘럼 배정' }))

  expect(await screen.findByText('이미 이 출강처에 배정된 커리큘럼입니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
})

it('says the previous assignment was reactivated and shows the refetched memo', async () => {
  const user = userEvent.setup()
  const reactivated = {
    ...assignedLearning,
    locations: [
      {
        ...assignedLearning.locations[0],
        studentCurriculums: [{ ...assignedLearning.locations[0].studentCurriculums[0], memo: '이전메모', reenrolled: true }],
      },
    ],
  }
  vi.mocked(getStudentLearning).mockResolvedValueOnce({ ...emptyLearning, locations: [studioOnly] }).mockResolvedValueOnce(reactivated)
  vi.mocked(assignStudentCurriculum).mockResolvedValue({
    studentCurriculumId: 7,
    curriculumId: 3,
    reenrolled: true,
    memo: '이전메모',
  })
  renderPage()

  await user.selectOptions(await screen.findByLabelText('배정할 커리큘럼 (연습실)'), '3')
  await user.type(screen.getByLabelText('배정 메모'), '새메모')
  await user.click(screen.getByRole('button', { name: '커리큘럼 배정' }))

  expect(await screen.findByText(/기존 커리큘럼 배정을 다시 활성화했습니다/)).toBeTruthy()
  expect(screen.getByLabelText('메모').getAttribute('value') ?? (screen.getByLabelText('메모') as HTMLInputElement).value).toBe('이전메모')
  expect(screen.queryByDisplayValue('새메모')).toBeNull()
})

it('releases a curriculum assignment and refetches learning', async () => {
  const user = userEvent.setup()
  vi.mocked(getStudentLearning)
    .mockResolvedValueOnce(assignedLearning)
    .mockResolvedValueOnce({ ...emptyLearning, locations: [studioOnly] })
  vi.mocked(releaseStudentCurriculum).mockResolvedValue(undefined)
  renderPage()

  await user.click(await screen.findByRole('button', { name: '배정 해제' }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog.textContent).toContain('모니터링과 과제 기록은 삭제되지 않습니다')
  expect(releaseStudentCurriculum).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '배정 해제' }))

  expect(releaseStudentCurriculum).toHaveBeenCalledWith(90, 7)
  expect(await screen.findByText('커리큘럼 배정을 해제했습니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
})

it('keeps a curriculum available at another location', async () => {
  vi.mocked(getStudentLearning).mockResolvedValue({
    ...emptyLearning,
    locations: [
      assignedLearning.locations[0],
      {
        teacherStudentLocationId: 91,
        locationId: 31,
        locationName: '합주실',
        address: null,
        studentCurriculums: [],
      },
    ],
  })
  renderPage()

  const other = await screen.findByLabelText('배정할 커리큘럼 (합주실)')
  expect(within(other).getByRole('option', { name: '기초' })).toBeTruthy()
})

it('patches the curriculum memo and refetches learning', async () => {
  const user = userEvent.setup()
  const withMemo = {
    ...assignedLearning,
    locations: [
      {
        ...assignedLearning.locations[0],
        studentCurriculums: [{ ...assignedLearning.locations[0].studentCurriculums[0], memo: '저장메모' }],
      },
    ],
  }
  vi.mocked(getStudentLearning).mockResolvedValueOnce(assignedLearning).mockResolvedValueOnce(withMemo)
  vi.mocked(updateStudentCurriculumMemo).mockResolvedValue({
    studentCurriculumId: 7,
    curriculumId: 3,
    reenrolled: false,
    memo: '저장메모',
  })
  renderPage()

  const memo = await screen.findByLabelText('메모')
  await user.type(memo, '저장메모')
  await user.click(screen.getByRole('button', { name: '메모 저장' }))

  expect(updateStudentCurriculumMemo).toHaveBeenCalledWith(90, 7, '저장메모')
  expect(await screen.findByText('메모를 저장했습니다.')).toBeTruthy()
  expect(getStudentLearning).toHaveBeenCalledTimes(2)
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
