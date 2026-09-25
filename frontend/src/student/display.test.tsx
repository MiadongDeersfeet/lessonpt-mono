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

it('maps progress status labels without changing the API value', () => {
  expect(progressStatusLabel('YET')).toBe('시작 전')
  expect(progressStatusLabel('IN_PROGRESS')).toBe('진행 중')
  expect(progressStatusLabel('COMPLETED')).toBe('완료')
  expect(progressStatusLabel('STOPPED')).toBe('중단')
})
