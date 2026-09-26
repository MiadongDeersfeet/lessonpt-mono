import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/apiClient.ts'
import type { ContentDetail, ContentResource } from '../../types/curriculum.ts'
import { ContentResources } from './ContentResources.tsx'

vi.mock('../../api/resourceApi.ts', () => ({
  uploadContentResource: vi.fn(),
  deleteContentResource: vi.fn(),
  fetchTeacherResourceBlob: vi.fn(),
}))

import { deleteContentResource, fetchTeacherResourceBlob, uploadContentResource } from '../../api/resourceApi.ts'

const sheet: ContentResource = {
  resourceId: 4,
  resourceType: 'SHEET',
  originalFileName: 'notes.pdf',
  contentType: 'application/pdf',
  fileSize: 2048,
  createdAt: '2026-09-26T01:00:00',
}

const audio: ContentResource = {
  resourceId: 5,
  resourceType: 'AUDIO',
  originalFileName: 'groove.mp3',
  contentType: 'audio/mpeg',
  fileSize: 4096,
  createdAt: '2026-09-26T01:00:00',
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

it('shows empty sheet and audio states', () => {
  renderSlot(content())
  expect(screen.getByText('악보 없음')).toBeTruthy()
  expect(screen.getByText('음원 없음')).toBeTruthy()
  expect(screen.getByLabelText('PDF 업로드')).toBeTruthy()
  expect(screen.getByLabelText('음원 업로드')).toBeTruthy()
})

it('renders sheet and audio metadata', () => {
  vi.mocked(fetchTeacherResourceBlob).mockResolvedValue(new Blob(['audio']))
  renderSlot(content({ sheet, audio }))
  expect(screen.getByText('notes.pdf')).toBeTruthy()
  expect(screen.getByText('2.0 KB')).toBeTruthy()
  expect(screen.getByText('groove.mp3')).toBeTruthy()
  expect(screen.getByText('4.0 KB')).toBeTruthy()
})

it('uploads a pdf and replaces an existing sheet without removing it first', async () => {
  const user = userEvent.setup()
  const onContentChange = vi.fn()
  const replaced = { ...sheet, resourceId: 8, originalFileName: 'next.pdf' }
  vi.mocked(uploadContentResource).mockResolvedValue({ resource: replaced, storageWarning: false })
  renderSlot(content({ sheet }), onContentChange)

  await user.upload(screen.getByLabelText('교체'), new File(['%PDF'], 'next.pdf', { type: 'application/pdf' }))

  expect(uploadContentResource).toHaveBeenCalledWith(3, 8, 15, 'sheet', expect.any(File))
  expect(onContentChange).toHaveBeenCalledWith(expect.objectContaining({ sheet: replaced, audio: null }))
  expect(screen.getByText('notes.pdf')).toBeTruthy()
})

it('rejects a pdf over 20MB before upload', async () => {
  const user = userEvent.setup()
  renderSlot(content())
  const file = new File(['x'], 'big.pdf', { type: 'application/pdf' })
  Object.defineProperty(file, 'size', { value: 20 * 1024 * 1024 + 1 })
  await user.upload(screen.getByLabelText('PDF 업로드'), file)
  expect(await screen.findByText('PDF 20MB 초과')).toBeTruthy()
  expect(uploadContentResource).not.toHaveBeenCalled()
})

it('rejects audio over 50MB before upload', async () => {
  const user = userEvent.setup()
  renderSlot(content())
  const file = new File(['x'], 'big.mp3', { type: 'audio/mpeg' })
  Object.defineProperty(file, 'size', { value: 50 * 1024 * 1024 + 1 })
  await user.upload(screen.getByLabelText('음원 업로드'), file)
  expect(await screen.findByText('Audio 50MB 초과')).toBeTruthy()
  expect(uploadContentResource).not.toHaveBeenCalled()
})

it('keeps the current resource when upload fails', async () => {
  const user = userEvent.setup()
  const onContentChange = vi.fn()
  vi.mocked(uploadContentResource).mockRejectedValue(new ApiError(400, 'RESOURCE_INVALID_FILE', 'bad', null, []))
  renderSlot(content({ sheet }), onContentChange)
  await user.upload(screen.getByLabelText('교체'), new File(['nope'], 'bad.pdf', { type: 'application/pdf' }))
  expect(await screen.findByText('PDF 형식 오류')).toBeTruthy()
  expect(onContentChange).not.toHaveBeenCalled()
  expect(screen.getByText('notes.pdf')).toBeTruthy()
})

it('shows the storage limit message and keeps the sheet', async () => {
  const user = userEvent.setup()
  const onContentChange = vi.fn()
  vi.mocked(uploadContentResource).mockRejectedValue(
    new ApiError(409, 'RESOURCE_STORAGE_LIMIT_EXCEEDED', 'limit', null, []),
  )
  renderSlot(content({ sheet }), onContentChange)
  await user.upload(screen.getByLabelText('교체'), new File(['%PDF'], 'next.pdf', { type: 'application/pdf' }))
  expect(await screen.findByText('저장 용량 한도에 도달해 파일을 올릴 수 없습니다.')).toBeTruthy()
  expect(onContentChange).not.toHaveBeenCalled()
  expect(screen.getByText('notes.pdf')).toBeTruthy()
})

it('shows a storage warning from the response header', async () => {
  const user = userEvent.setup()
  vi.mocked(uploadContentResource).mockResolvedValue({ resource: sheet, storageWarning: true })
  renderSlot(content())
  await user.upload(screen.getByLabelText('PDF 업로드'), new File(['%PDF'], 'notes.pdf', { type: 'application/pdf' }))
  expect(await screen.findByText('저장 공간 사용량이 높습니다.')).toBeTruthy()
})

it('deletes a sheet only after confirmation', async () => {
  const user = userEvent.setup()
  const onContentChange = vi.fn()
  vi.mocked(deleteContentResource).mockResolvedValue(undefined)
  renderSlot(content({ sheet }), onContentChange)
  const slot = screen.getByRole('region', { name: '악보' })
  await user.click(within(slot).getByRole('button', { name: '삭제' }))
  expect(deleteContentResource).not.toHaveBeenCalled()
  const dialog = await screen.findByRole('dialog')
  await user.click(within(dialog).getByRole('button', { name: '삭제' }))
  expect(deleteContentResource).toHaveBeenCalledWith(3, 8, 15, 4)
  expect(onContentChange).toHaveBeenCalledWith(expect.objectContaining({ sheet: null }))
})

it('revokes a teacher preview blob url', async () => {
  const user = userEvent.setup()
  const revoke = vi.fn()
  vi.stubGlobal('URL', { createObjectURL: () => 'blob:preview', revokeObjectURL: revoke })
  vi.mocked(fetchTeacherResourceBlob).mockResolvedValue(new Blob(['%PDF']))
  const view = renderSlot(content({ sheet }))
  await user.click(screen.getByRole('button', { name: '악보 보기' }))
  expect(await screen.findByTitle('notes.pdf 보기')).toBeTruthy()
  view.unmount()
  expect(revoke).toHaveBeenCalledWith('blob:preview')
  vi.unstubAllGlobals()
})

function content(overrides: Partial<ContentDetail> = {}): ContentDetail {
  return {
    contentDetailId: 15,
    name: '싱글',
    displayOrder: 1,
    memo: null,
    targetBpm: null,
    evaluationMemo: null,
    youtubeUrl: null,
    sheet: null,
    audio: null,
    ...overrides,
  }
}

function renderSlot(value: ContentDetail, onContentChange = vi.fn()) {
  return render(
    <ContentResources curriculumId={3} categoryId={8} content={value} onContentChange={onContentChange} />,
  )
}
