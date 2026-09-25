import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import type { StudentPortalContent } from '../../types/studentPortal.ts'
import { StudentLessonResources } from './StudentLessonResources.tsx'

vi.mock('../../api/resourceApi.ts', async () => {
  const actual = await vi.importActual<typeof import('../../api/resourceApi.ts')>('../../api/resourceApi.ts')
  return {
    ...actual,
    fetchStudentResourceBlob: vi.fn(),
  }
})

const { fetchStudentResourceBlob } = await import('../../api/resourceApi.ts')

const sheet = {
  resourceId: 4,
  resourceType: 'SHEET' as const,
  originalFileName: 'notes.pdf',
  contentType: 'application/pdf',
  fileSize: 1200,
  createdAt: '2026-09-26T01:00:00',
}

const audio = {
  resourceId: 5,
  resourceType: 'AUDIO' as const,
  originalFileName: 'groove.mp3',
  contentType: 'audio/mpeg',
  fileSize: 2400,
  createdAt: '2026-09-26T01:00:00',
}

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
  vi.unstubAllGlobals()
})

it('hides sheet and audio when they are absent', () => {
  render(<StudentLessonResources content={portalContent()} />)
  expect(screen.queryByRole('region', { name: '악보' })).toBeNull()
  expect(screen.queryByRole('region', { name: '음원' })).toBeNull()
  expect(screen.queryByTitle('수업 영상')).toBeNull()
  expect(fetchStudentResourceBlob).not.toHaveBeenCalled()
})

it('previews a student pdf from a blob url and keeps audio on the direct resource url', async () => {
  const revoke = vi.fn()
  stubBlobUrls(() => 'blob:preview', revoke)
  vi.mocked(fetchStudentResourceBlob).mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))

  render(<StudentLessonResources content={portalContent({ sheet, audio, youtubeUrl: 'https://youtu.be/abcdefghijk' })} />)

  expect(screen.getByRole('status').textContent).toBe('악보를 불러오는 중')
  expect(screen.queryByTitle('notes.pdf 보기')).toBeNull()
  const preview = await screen.findByTitle('notes.pdf 보기')
  expect(preview.getAttribute('src')).toBe('blob:preview')
  expect(preview.getAttribute('src')).not.toContain('/api/v1/student/resources/')
  expect(fetchStudentResourceBlob).toHaveBeenCalledWith(4, 'inline')
  expect(screen.getByRole('link', { name: '악보 다운로드' }).getAttribute('href')).toBe(
    '/api/v1/student/resources/4/content?disposition=attachment',
  )
  expect(screen.getByRole('link', { name: '음원 다운로드' }).getAttribute('href')).toBe(
    '/api/v1/student/resources/5/content?disposition=attachment',
  )
  const player = document.querySelector('audio')
  expect(player?.getAttribute('src')).toBe('/api/v1/student/resources/5/content?disposition=inline')
  expect(screen.getByTitle('수업 영상').getAttribute('src')).toBe('https://www.youtube-nocookie.com/embed/abcdefghijk')
  expect(document.body.textContent).not.toContain('Bearer')
})

it('revokes the pdf blob url when the resource changes and when the preview unmounts', async () => {
  const revoke = vi.fn()
  let created = 0
  stubBlobUrls(() => `blob:preview-${++created}`, revoke)
  vi.mocked(fetchStudentResourceBlob).mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }))

  const view = render(
    <StudentLessonResources content={portalContent({ sheet, audio })} />,
  )
  expect((await screen.findByTitle('notes.pdf 보기')).getAttribute('src')).toBe('blob:preview-1')

  view.rerender(
    <StudentLessonResources
      content={portalContent({
        sheet: { ...sheet, resourceId: 9, originalFileName: 'next.pdf' },
        audio,
      })}
    />,
  )
  expect((await screen.findByTitle('next.pdf 보기')).getAttribute('src')).toBe('blob:preview-2')
  expect(revoke).toHaveBeenCalledWith('blob:preview-1')
  expect(document.querySelector('audio')?.getAttribute('src')).toBe(
    '/api/v1/student/resources/5/content?disposition=inline',
  )

  view.unmount()
  expect(revoke).toHaveBeenCalledWith('blob:preview-2')
})

it('shows a resource error and no iframe when the pdf fetch fails', async () => {
  vi.mocked(fetchStudentResourceBlob).mockRejectedValue(new Error('IllegalArgumentException: range parse failed'))

  render(<StudentLessonResources content={portalContent({ sheet })} />)

  expect((await screen.findByRole('alert')).textContent).toBe('악보를 불러오지 못했습니다.')
  expect(screen.queryByTitle('notes.pdf 보기')).toBeNull()
  expect(document.body.textContent).not.toContain('IllegalArgumentException')
  await waitFor(() => {
    expect(fetchStudentResourceBlob).toHaveBeenCalledWith(4, 'inline')
  })
})

it('does not use an unexpected youtube value as an iframe src', () => {
  render(<StudentLessonResources content={portalContent({ youtubeUrl: 'https://example.com/watch?v=abcdefghijk' })} />)
  expect(screen.queryByTitle('수업 영상')).toBeNull()
})

function stubBlobUrls(create: () => string, revoke: (url: string) => void) {
  URL.createObjectURL = create
  URL.revokeObjectURL = revoke
}

function portalContent(overrides: Partial<StudentPortalContent> = {}): StudentPortalContent {
  return {
    name: '싱글',
    targetBpm: null,
    currentBpm: null,
    progressStatus: null,
    sheetUrl: null,
    youtubeUrl: null,
    audioUrl: null,
    sheet: null,
    audio: null,
    homeworks: [],
    ...overrides,
  }
}
