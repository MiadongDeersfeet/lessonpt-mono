import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { ResourceButtons, type ResourceContent } from './ResourceButtons.tsx'

const sheet = {
  resourceId: 1,
  resourceType: 'SHEET' as const,
  originalFileName: 'score.pdf',
  contentType: 'application/pdf',
  fileSize: 10,
  createdAt: '2026-09-01T00:00:00',
}

afterEach(() => {
  cleanup()
})

it('shows only the resources that exist', () => {
  render(<ResourceButtons content={content({ sheet, audio: null, youtubeUrl: null })} />)
  expect(screen.getByRole('button', { name: '악보 보기' })).toBeTruthy()
  expect(screen.queryByRole('button', { name: '영상 보기' })).toBeNull()
  expect(screen.queryByRole('button', { name: '음원 보기' })).toBeNull()
  expect(screen.queryByText('—')).toBeNull()
  expect(screen.queryByText('자료 없음')).toBeNull()
})

it('shows one empty label when a learning row has no resources', () => {
  render(<ResourceButtons content={content({ sheet: null, audio: null, youtubeUrl: null })} />)
  expect(screen.getAllByText('자료 없음')).toHaveLength(1)
  expect(screen.queryByText('—')).toBeNull()
})

it('leaves a missing column blank instead of a dash', () => {
  const view = render(<ResourceButtons only="audio" content={content({ sheet, audio: null, youtubeUrl: null })} />)
  expect(view.container.textContent).toBe('')
  expect(screen.queryByText('—')).toBeNull()
  expect(screen.queryByText('자료 없음')).toBeNull()
})

function content(values: Pick<ResourceContent, 'sheet' | 'audio' | 'youtubeUrl'>): ResourceContent {
  return { name: '싱글', ...values }
}
