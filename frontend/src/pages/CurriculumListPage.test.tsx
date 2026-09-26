import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ApiError } from '../api/apiClient.ts'
import { CurriculumListPage } from './CurriculumListPage.tsx'

vi.mock('../api/curriculumApi.ts', () => ({
  listCurriculums: vi.fn(),
  createCurriculum: vi.fn(),
  updateCurriculum: vi.fn(),
  deactivateCurriculum: vi.fn(),
  listCategories: vi.fn(),
  listContentDetails: vi.fn(),
}))

import {
  createCurriculum,
  deactivateCurriculum,
  listCategories,
  listContentDetails,
  listCurriculums,
  updateCurriculum,
} from '../api/curriculumApi.ts'

const listed = { curriculumId: 3, name: '기초', displayOrder: 1 }

afterEach(() => {
  cleanup()
})

beforeEach(() => {
  vi.mocked(listCurriculums).mockReset()
  vi.mocked(createCurriculum).mockReset()
  vi.mocked(updateCurriculum).mockReset()
  vi.mocked(deactivateCurriculum).mockReset()
  vi.mocked(listCategories).mockReset()
  vi.mocked(listContentDetails).mockReset()
  vi.mocked(listCurriculums).mockResolvedValue([listed])
})

it('creates a curriculum from the response without loading the tree', async () => {
  const user = userEvent.setup()
  vi.mocked(createCurriculum).mockResolvedValue({ curriculumId: 4, name: '새과정', displayOrder: 2 })
  renderPage()

  await user.click(await screen.findByRole('button', { name: '커리큘럼 추가' }))
  await user.type(screen.getByLabelText('이름'), '새과정')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('새과정')).toBeTruthy()
  expect(createCurriculum).toHaveBeenCalledWith('새과정')
  expect(listCurriculums).toHaveBeenCalledTimes(1)
  expect(listCategories).not.toHaveBeenCalled()
  expect(listContentDetails).not.toHaveBeenCalled()
  expect(screen.queryByRole('button', { name: '복구' })).toBeNull()
})

it('updates the listed name from the patch response', async () => {
  const user = userEvent.setup()
  vi.mocked(updateCurriculum).mockResolvedValue({ ...listed, name: '수정과정' })
  renderPage()

  await user.click(await screen.findByRole('button', { name: '편집' }))
  const dialog = await screen.findByRole('dialog')
  expect(within(dialog).queryByLabelText('순서')).toBeNull()
  const name = within(dialog).getByLabelText('이름')
  await user.clear(name)
  await user.type(name, '수정과정')
  await user.click(within(dialog).getByRole('button', { name: '저장' }))

  expect(await screen.findByText('수정과정')).toBeTruthy()
  expect(updateCurriculum).toHaveBeenCalledWith(3, '수정과정')
  expect(listCurriculums).toHaveBeenCalledTimes(1)
})

it('asks before deactivating and then refetches the curriculum list', async () => {
  const user = userEvent.setup()
  vi.mocked(deactivateCurriculum).mockResolvedValue(undefined)
  vi.mocked(listCurriculums)
    .mockResolvedValueOnce([listed, { curriculumId: 5, name: '다음과정', displayOrder: 2 }])
    .mockResolvedValueOnce([{ curriculumId: 5, name: '다음과정', displayOrder: 1 }])
  renderPage()

  const row = (await screen.findByText('기초')).closest('tr')
  if (!row) {
    throw new Error('curriculum row missing')
  }
  await user.click(within(row).getByRole('button', { name: '커리큘럼 삭제' }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog.textContent).toContain('활성 카테고리와 내용')
  expect(dialog.textContent).toContain('자동으로 돌아오지 않습니다')
  expect(deactivateCurriculum).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '삭제' }))

  expect(deactivateCurriculum).toHaveBeenCalledWith(3)
  expect(await screen.findByText('다음과정')).toBeTruthy()
  expect(screen.queryByText('기초')).toBeNull()
  expect(listCurriculums).toHaveBeenCalledTimes(2)
  expect(listCategories).not.toHaveBeenCalled()
})

it('shows a neutral order conflict message', async () => {
  const user = userEvent.setup()
  vi.mocked(deactivateCurriculum).mockRejectedValue(
    new ApiError(409, 'ORDER_CONFLICT', '다른 요청이 장소 순서를 변경 중입니다. 잠시 후 다시 시도해 주세요.', 'trace-8', []),
  )
  renderPage()

  await user.click(await screen.findByRole('button', { name: '커리큘럼 삭제' }))
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: '삭제' }))

  expect(await screen.findByText('다른 요청이 순서를 변경 중입니다. 잠시 후 다시 시도해 주세요.')).toBeTruthy()
  expect(screen.queryByText('장소 순서')).toBeNull()
  expect(listCurriculums).toHaveBeenCalledTimes(1)
})

function renderPage() {
  render(
    <MemoryRouter>
      <CurriculumListPage />
    </MemoryRouter>,
  )
}
