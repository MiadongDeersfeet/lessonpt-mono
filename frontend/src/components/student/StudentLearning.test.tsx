import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, it } from 'vitest'
import type { StudentPortalContent, StudentPortalLearning } from '../../types/studentPortal.ts'
import { StudentLearning } from './StudentLearning.tsx'

afterEach(() => {
  cleanup()
  localStorage.clear()
})

it('shows monitored lessons and only a count for unpublished details', () => {
  render(<StudentLearning learning={learning([{
    name: 'Rudiments',
    totalContentCount: 8,
    contents: [
      lesson({ monitoringId: 1, name: 'Single Stroke', progressStatus: 'IN_PROGRESS' }),
      lesson({ monitoringId: 2, name: 'Double Stroke', progressStatus: 'YET' }),
      lesson({ monitoringId: 3, name: 'Paradiddle', progressStatus: 'COMPLETED' }),
    ],
  }])} />)

  expect(screen.getByRole('button', { name: /Rudiments/ }).textContent).toContain('3 / 8 공개')
  expect(screen.getByRole('heading', { name: 'Single Stroke' })).toBeTruthy()
  expect(screen.getByText('진행 중')).toBeTruthy()
  expect(screen.getByText('시작 전')).toBeTruthy()
  expect(screen.queryByText('비공개 루디먼트')).toBeNull()
  const locked = screen.getByText('아직 공개되지 않은 학습 내용 5개')
  expect(locked.className).toBe('learning-locked')
  expect(locked.querySelector('.status-label')).toBeNull()
  expect(screen.queryByText('다음 학습 내용')).toBeNull()
})

it('keeps an empty category visible without lesson details', async () => {
  const user = userEvent.setup()
  render(<StudentLearning learning={learning([{
    name: 'Coordination',
    totalContentCount: 5,
    contents: [],
  }])} />)

  const toggle = screen.getByRole('button', { name: /Coordination/ })
  expect(toggle.textContent).toContain('0 / 5 공개')
  expect(toggle.getAttribute('aria-expanded')).toBe('true')
  expect(screen.getByText('아직 공개되지 않은 학습 내용 5개')).toBeTruthy()
  await user.click(toggle)
  expect(toggle.getAttribute('aria-expanded')).toBe('false')
  expect(screen.queryByText('아직 공개되지 않은 학습 내용 5개')).toBeNull()
  toggle.focus()
  await user.keyboard('{Enter}')
  expect(toggle.getAttribute('aria-expanded')).toBe('true')
  expect(screen.getByText('아직 공개되지 않은 학습 내용 5개')).toBeTruthy()
})

it('lists a few locked placeholders without a progress status', () => {
  render(<StudentLearning learning={learning([{
    name: 'Groove',
    totalContentCount: 3,
    contents: [lesson({ monitoringId: 9, name: 'Shuffle', progressStatus: 'IN_PROGRESS' })],
  }])} />)

  expect(screen.getByRole('button', { name: /Groove/ }).textContent).toContain('1 / 3 공개')
  expect(screen.getAllByText('다음 학습 내용')).toHaveLength(2)
  expect(screen.queryByText(/아직 공개되지 않은 학습 내용/)).toBeNull()
  for (const placeholder of screen.getAllByText('다음 학습 내용')) {
    expect(placeholder.className).not.toContain('status-')
  }
  expect(screen.getByText('진행 중').className).toContain('status-IN_PROGRESS')
})

it('shows homework completion only when the value is boolean', () => {
  render(<StudentLearning learning={learning([{
    name: 'Rudiments',
    totalContentCount: 2,
    contents: [
      lesson({
        monitoringId: 1,
        name: 'Single Stroke',
        homeworks: [{ homeworkId: 4, content: '120 BPM에서 3분씩 연습', deadline: '2026-10-03T00:00:00', completed: null, feedback: '좋아요' }],
      }),
      lesson({
        monitoringId: 2,
        name: 'Double Stroke',
        homeworks: [{ homeworkId: 5, content: '메트로놈', deadline: null, completed: false, feedback: null }],
      }),
    ],
  }])} />)

  const unclear = screen.getByRole('heading', { name: 'Single Stroke' }).closest('article')
  expect(unclear?.textContent).toContain('120 BPM에서 3분씩 연습')
  expect(unclear?.textContent).toContain('기한 2026-10-03')
  expect(unclear?.textContent).toContain('피드백 좋아요')
  expect(unclear?.textContent).not.toContain('미완료')
  expect(unclear?.textContent).not.toContain('완료')
  expect(screen.getByRole('heading', { name: 'Double Stroke' }).closest('article')?.textContent).toContain('미완료')
})

it('keeps open video with the same lesson when the list order changes', async () => {
  const user = userEvent.setup()
  const single = lesson({
    monitoringId: 1,
    name: '싱글',
    youtubeUrl: 'https://youtu.be/abcdefghijk',
  })
  const double = lesson({
    monitoringId: 2,
    name: '더블',
    youtubeUrl: 'https://youtu.be/zyxwvutsrqp',
  })
  const view = render(<StudentLearning learning={learning([{ name: '루디먼트', totalContentCount: 2, contents: [single, double] }])} />)

  const singleItem = screen.getByRole('heading', { name: '싱글' }).closest('article')
  if (!singleItem) {
    throw new Error('missing lesson')
  }
  await user.click(within(singleItem).getByRole('button', { name: '영상 보기' }))
  expect(within(singleItem).getByTitle('수업 영상').getAttribute('src')).toBe('https://www.youtube-nocookie.com/embed/abcdefghijk')

  view.rerender(<StudentLearning learning={learning([{ name: '루디먼트', totalContentCount: 2, contents: [double, single] }])} />)
  const singleAfter = screen.getByRole('heading', { name: '싱글' }).closest('article')
  const doubleAfter = screen.getByRole('heading', { name: '더블' }).closest('article')
  if (!singleAfter || !doubleAfter) {
    throw new Error('missing lesson')
  }
  expect(within(singleAfter).getByTitle('수업 영상').getAttribute('src')).toBe('https://www.youtube-nocookie.com/embed/abcdefghijk')
  expect(within(doubleAfter).queryByTitle('수업 영상')).toBeNull()
})

it('keeps the last category open state after leaving the page', async () => {
  const user = userEvent.setup()
  const categories = [
    { name: 'Coordination', totalContentCount: 5, contents: [] },
    { name: 'Groove', totalContentCount: 1, contents: [lesson({ monitoringId: 9, name: 'Shuffle' })] },
  ]
  const first = render(<StudentLearning learning={learning(categories)} />)
  await user.click(screen.getByRole('button', { name: /Coordination/ }))
  first.unmount()

  const second = render(<StudentLearning learning={learning(categories)} />)
  const closed = screen.getByRole('button', { name: /Coordination/ })
  const stillOpen = screen.getByRole('button', { name: /Groove/ })
  expect(closed.getAttribute('aria-expanded')).toBe('false')
  expect(screen.queryByText('아직 공개되지 않은 학습 내용 5개')).toBeNull()
  expect(stillOpen.getAttribute('aria-expanded')).toBe('true')
  expect(screen.getByRole('heading', { name: 'Shuffle' })).toBeTruthy()

  await user.click(closed)
  second.unmount()
  render(<StudentLearning learning={learning(categories)} />)
  expect(screen.getByRole('button', { name: /Coordination/ }).getAttribute('aria-expanded')).toBe('true')
  expect(screen.getByText('아직 공개되지 않은 학습 내용 5개')).toBeTruthy()
})

function learning(categories: StudentPortalLearning['curriculums'][number]['categories']): StudentPortalLearning {
  return {
    curriculums: [{ name: 'Drum Fundamentals', progress: { completedCount: 1, totalCount: 8, percentage: 12.5 }, categories }],
  }
}

function lesson(overrides: Partial<StudentPortalContent> & Pick<StudentPortalContent, 'monitoringId' | 'name'>): StudentPortalContent {
  return {
    targetBpm: 160,
    currentBpm: 120,
    progressStatus: 'IN_PROGRESS',
    youtubeUrl: null,
    sheet: null,
    audio: null,
    homeworks: [],
    ...overrides,
  }
}
