import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import type { ContentDetailWriteBody } from '../types/curriculum.ts'
import { CurriculumBuilderPage } from './CurriculumBuilderPage.tsx'

vi.mock('../api/curriculumApi.ts', () => ({
  getCurriculum: vi.fn(),
  updateCurriculum: vi.fn(),
  listCategories: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
  deactivateCategory: vi.fn(),
  listContentDetails: vi.fn(),
  createContentDetail: vi.fn(),
  updateContentDetail: vi.fn(),
  deactivateContentDetail: vi.fn(),
}))

import {
  createCategory,
  createContentDetail,
  deactivateCategory,
  deactivateContentDetail,
  getCurriculum,
  listCategories,
  listContentDetails,
  updateCategory,
  updateContentDetail,
} from '../api/curriculumApi.ts'

const curriculum = { curriculumId: 3, name: '기초', displayOrder: 1 }
const rudiment = { categoryId: 8, name: '루디먼트', displayOrder: 1 }
const reading = { categoryId: 9, name: '악보', displayOrder: 2 }
const stroke = {
  contentDetailId: 15,
  name: '싱글',
  displayOrder: 1,
  memo: '천천히',
  targetBpm: 80,
  evaluationMemo: '손목',
  sheetUrl: 'https://sheet.example/a',
  youtubeUrl: null,
  audioUrl: null,
  sheet: null,
  audio: null,
}

afterEach(() => {
  cleanup()
})

beforeEach(() => {
  vi.mocked(getCurriculum).mockReset()
  vi.mocked(listCategories).mockReset()
  vi.mocked(listContentDetails).mockReset()
  vi.mocked(createCategory).mockReset()
  vi.mocked(updateCategory).mockReset()
  vi.mocked(deactivateCategory).mockReset()
  vi.mocked(createContentDetail).mockReset()
  vi.mocked(updateContentDetail).mockReset()
  vi.mocked(deactivateContentDetail).mockReset()
  vi.mocked(getCurriculum).mockResolvedValue(curriculum)
  vi.mocked(listCategories).mockResolvedValue([rudiment, reading])
  vi.mocked(listContentDetails).mockImplementation(async (_curriculumId, categoryId) => {
    if (categoryId === 8) {
      return [stroke]
    }
    return []
  })
})

it('loads content details once per category and has no restore or order editor', async () => {
  renderPage()

  expect(await screen.findByText('싱글')).toBeTruthy()
  expect(listContentDetails).toHaveBeenCalledTimes(2)
  expect(listContentDetails).toHaveBeenCalledWith(3, 8)
  expect(listContentDetails).toHaveBeenCalledWith(3, 9)
  expect(screen.queryByRole('button', { name: '복구' })).toBeNull()
  expect(screen.queryByLabelText('순서')).toBeNull()
  expect(screen.queryByRole('spinbutton')).toBeNull()
})

it('creates, edits, and deactivates a category and drops its contents', async () => {
  const user = userEvent.setup()
  vi.mocked(createCategory).mockResolvedValue({ categoryId: 10, name: '새분류', displayOrder: 3 })
  vi.mocked(updateCategory).mockResolvedValue({ ...rudiment, name: '수정분류' })
  vi.mocked(deactivateCategory).mockResolvedValue(undefined)
  vi.mocked(listCategories)
    .mockResolvedValueOnce([rudiment, reading])
    .mockResolvedValueOnce([reading])
  renderPage()

  await screen.findByText('싱글')
  await user.click(screen.getByRole('button', { name: '카테고리 추가' }))
  await user.type(screen.getByLabelText('이름'), '새분류')
  await user.click(screen.getByRole('button', { name: '저장' }))
  expect(await screen.findByText(/새분류/)).toBeTruthy()
  expect(createCategory).toHaveBeenCalledWith(3, '새분류')

  const category = (await screen.findByRole('heading', { name: '1. 루디먼트' })).parentElement
  if (!category) {
    throw new Error('category missing')
  }
  await user.click(within(category).getByRole('button', { name: '편집' }))
  const editDialog = await screen.findByRole('dialog')
  const name = within(editDialog).getByLabelText('이름')
  await user.clear(name)
  await user.type(name, '수정분류')
  await user.click(within(editDialog).getByRole('button', { name: '저장' }))
  expect(await screen.findByText(/수정분류/)).toBeTruthy()
  expect(updateCategory).toHaveBeenCalledWith(3, 8, '수정분류')

  const edited = (await screen.findByRole('heading', { name: '1. 수정분류' })).parentElement
  if (!edited) {
    throw new Error('edited category missing')
  }
  await user.click(within(edited).getByRole('button', { name: '비활성화' }))
  const dialog = await screen.findByRole('dialog')
  expect(dialog.textContent).toContain('활성 내용')
  expect(deactivateCategory).not.toHaveBeenCalled()
  await user.click(within(dialog).getByRole('button', { name: '비활성화' }))

  expect(deactivateCategory).toHaveBeenCalledWith(3, 8)
  expect(await screen.findByText('카테고리를 비활성화했습니다.')).toBeTruthy()
  expect(screen.queryByText('싱글')).toBeNull()
  expect(listCategories).toHaveBeenCalledTimes(2)
})

it('creates content with a null target bpm and null urls', async () => {
  const user = userEvent.setup()
  vi.mocked(createContentDetail).mockResolvedValue({
    contentDetailId: 16,
    name: '더블',
    displayOrder: 1,
    memo: null,
    targetBpm: null,
    evaluationMemo: null,
    sheetUrl: null,
    youtubeUrl: null,
    audioUrl: null,
    sheet: null,
    audio: null,
  })
  renderPage()

  const readingCard = (await screen.findByText(/악보/)).closest('section')
  if (!readingCard) {
    throw new Error('reading category missing')
  }
  await user.click(within(readingCard).getByRole('button', { name: '내용 추가' }))
  await user.type(screen.getByLabelText('이름'), '더블')
  await user.click(screen.getByRole('button', { name: '저장' }))

  expect(await screen.findByText('더블')).toBeTruthy()
  expect(createContentDetail).toHaveBeenCalledWith(3, 9, emptyContent('더블'))
})

it('blocks target bpm outside 60 to 240 and sends the boundary values', async () => {
  const user = userEvent.setup()
  vi.mocked(createContentDetail).mockImplementation(async (_curriculumId, _categoryId, body) => ({
    contentDetailId: 20,
    name: body.name,
    displayOrder: 1,
    memo: body.memo,
    targetBpm: body.targetBpm,
    evaluationMemo: body.evaluationMemo,
    sheetUrl: null,
    youtubeUrl: body.youtubeUrl,
    audioUrl: null,
    sheet: null,
    audio: null,
  }))
  renderPage()

  const readingCard = (await screen.findByText(/악보/)).closest('section')
  if (!readingCard) {
    throw new Error('reading category missing')
  }
  await user.click(within(readingCard).getByRole('button', { name: '내용 추가' }))
  await user.type(screen.getByLabelText('이름'), '템포')
  const bpm = screen.getByLabelText('목표 BPM')

  await user.type(bpm, '59')
  await user.click(screen.getByRole('button', { name: '저장' }))
  expect(await screen.findByText('목표 BPM은 60 이상 240 이하여야 합니다.')).toBeTruthy()
  expect(createContentDetail).not.toHaveBeenCalled()

  await user.clear(bpm)
  await user.type(bpm, '241')
  await user.click(screen.getByRole('button', { name: '저장' }))
  expect(createContentDetail).not.toHaveBeenCalled()

  await user.clear(bpm)
  await user.type(bpm, '60')
  await user.click(screen.getByRole('button', { name: '저장' }))
  expect(createContentDetail).toHaveBeenCalledWith(3, 9, emptyContent('템포', 60))

  await user.click(within(readingCard).getByRole('button', { name: '내용 추가' }))
  await user.type(screen.getByLabelText('이름'), '빠른템포')
  await user.type(screen.getByLabelText('목표 BPM'), '240')
  await user.click(screen.getByRole('button', { name: '저장' }))
  expect(createContentDetail).toHaveBeenCalledWith(3, 9, emptyContent('빠른템포', 240))
})

it('keeps the youtube field and does not ask for sheet or audio urls', async () => {
  const user = userEvent.setup()
  renderPage()

  const readingCard = (await screen.findByText(/악보/)).closest('section')
  if (!readingCard) {
    throw new Error('reading category missing')
  }
  await user.click(within(readingCard).getByRole('button', { name: '내용 추가' }))
  const dialog = await screen.findByRole('dialog')
  expect(within(dialog).getByLabelText('YouTube URL')).toBeTruthy()
  expect(within(dialog).queryByLabelText('악보 URL')).toBeNull()
  expect(within(dialog).queryByLabelText('오디오 URL')).toBeNull()
  expect(within(dialog).getByText('저장 후 파일을 추가할 수 있습니다.')).toBeTruthy()
})

it('updates a content detail and deactivates it after confirmation', async () => {
  const user = userEvent.setup()
  vi.mocked(updateContentDetail).mockResolvedValue({ ...stroke, name: '수정싱글', targetBpm: null })
  vi.mocked(deactivateContentDetail).mockResolvedValue(undefined)
  vi.mocked(listContentDetails).mockImplementation(async (_curriculumId, categoryId) => {
    if (categoryId === 8) {
      return [stroke]
    }
    return []
  })
  renderPage()

  const contentRow = (await screen.findByText('싱글')).closest('tr')
  if (!contentRow) {
    throw new Error('content row missing')
  }
  await user.click(within(contentRow).getByRole('button', { name: '편집' }))
  const dialog = await screen.findByRole('dialog')
  expect(within(dialog).queryByLabelText('순서')).toBeNull()
  const name = within(dialog).getByLabelText('이름')
  await user.clear(name)
  await user.type(name, '수정싱글')
  await user.clear(within(dialog).getByLabelText('목표 BPM'))
  await user.click(within(dialog).getByRole('button', { name: '저장' }))

  expect(await screen.findByText('수정싱글')).toBeTruthy()
  expect(updateContentDetail).toHaveBeenCalledWith(3, 8, 15, {
    name: '수정싱글',
    memo: '천천히',
    targetBpm: null,
    evaluationMemo: '손목',
    youtubeUrl: null,
  })

  vi.mocked(listContentDetails).mockResolvedValue([])
  const row = screen.getByText('수정싱글').closest('tr')
  if (!row) {
    throw new Error('content row missing')
  }
  await user.click(within(row).getByRole('button', { name: '비활성화' }))
  const confirm = await screen.findByRole('dialog')
  expect(confirm.textContent).toContain('모니터링과 과제 기록은 남습니다')
  expect(deactivateContentDetail).not.toHaveBeenCalled()
  await user.click(within(confirm).getByRole('button', { name: '비활성화' }))

  expect(deactivateContentDetail).toHaveBeenCalledWith(3, 8, 15)
  expect(await screen.findByText('내용을 비활성화했습니다.')).toBeTruthy()
  expect(screen.queryByText('수정싱글')).toBeNull()
  expect(listContentDetails).toHaveBeenCalledWith(3, 8)
})

function emptyContent(name: string, targetBpm: number | null = null): ContentDetailWriteBody {
  return {
    name,
    memo: null,
    targetBpm,
    evaluationMemo: null,
    youtubeUrl: null,
  }
}

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/curriculums/3']}>
      <Routes>
        <Route path="/curriculums/:curriculumId" element={<CurriculumBuilderPage />} />
      </Routes>
    </MemoryRouter>,
  )
}
