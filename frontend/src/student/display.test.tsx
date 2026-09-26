import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { ProgressValue } from '../components/student/ProgressValue.tsx'
import { progressStatusLabel } from './display.ts'

it('shows a missing progress separately from zero percent', () => {
  const { rerender } = render(<ProgressValue progress={null} />)
  expect(screen.getByText('진행률 계산 대상 없음')).toBeTruthy()

  rerender(<ProgressValue progress={{ completedCount: 0, totalCount: 4, percentage: 0 }} />)
  expect(screen.getByText('0.0%')).toBeTruthy()
})

it('draws a thin meter from the server percentage', () => {
  const view = render(<ProgressValue meter progress={{ completedCount: 0, totalCount: 4, percentage: 0 }} />)
  const fill = () => view.container.querySelector('.progress-meter-fill')?.getAttribute('style') ?? ''
  expect(view.container.querySelector('.progress-value')).toBeNull()
  expect(fill()).toContain('width: 0%')

  view.rerender(<ProgressValue meter progress={{ completedCount: 1, totalCount: 4, percentage: 35.7 }} />)
  expect(fill()).toContain('width: 35.7%')

  view.rerender(<ProgressValue meter progress={{ completedCount: 4, totalCount: 4, percentage: 100 }} />)
  expect(fill()).toContain('width: 100%')
})

it('maps progress status labels without changing the API value', () => {
  expect(progressStatusLabel('YET')).toBe('시작 전')
  expect(progressStatusLabel('IN_PROGRESS')).toBe('진행 중')
  expect(progressStatusLabel('COMPLETED')).toBe('완료')
  expect(progressStatusLabel('STOPPED')).toBe('중단')
})
