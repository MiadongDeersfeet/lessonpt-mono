import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { BpmGauge } from './BpmGauge.tsx'

afterEach(() => {
  cleanup()
})

it('shows a target comparison gauge when both bpm values exist', () => {
  render(<BpmGauge current={120} target={160} />)
  const meter = screen.getByRole('meter', { name: '목표 BPM 대비' })
  expect(meter.getAttribute('aria-valuenow')).toBe('75')
  expect(meter.querySelector('span')?.getAttribute('style')).toContain('width: 75%')
  expect(screen.getByText('120 → 160')).toBeTruthy()
  expect(screen.getByText('목표 BPM 대비 75%')).toBeTruthy()
})

it('does not draw a zero gauge when only the target exists', () => {
  render(<BpmGauge current={null} target={160} />)
  expect(screen.getByText('목표 160 BPM')).toBeTruthy()
  expect(screen.queryByRole('meter')).toBeNull()
})

it('shows only the current bpm when the target is missing', () => {
  render(<BpmGauge current={120} target={null} />)
  expect(screen.getByText('현재 120 BPM')).toBeTruthy()
  expect(screen.queryByRole('meter')).toBeNull()
})

it('does not divide by a zero target', () => {
  render(<BpmGauge current={10} target={0} />)
  expect(screen.getByText('현재 10 BPM')).toBeTruthy()
  expect(screen.queryByRole('meter')).toBeNull()
})

it('renders nothing when both bpm values are missing', () => {
  const view = render(<BpmGauge current={null} target={null} />)
  expect(view.container.textContent).toBe('')
})

it('caps the gauge at the target and marks it reached without calling it course progress', () => {
  render(<BpmGauge current={180} target={160} />)
  expect(screen.getByRole('meter').getAttribute('aria-valuenow')).toBe('100')
  expect(screen.getByText('목표 BPM 대비 100% · 목표 달성')).toBeTruthy()
  expect(screen.queryByText('완료')).toBeNull()
})
