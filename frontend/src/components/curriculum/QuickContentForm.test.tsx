import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, expect, it, vi } from 'vitest'
import { QuickContentForm } from './QuickContentForm.tsx'
import { createContentDetail } from '../../api/curriculumApi.ts'
vi.mock('../../api/curriculumApi.ts', () => ({ createContentDetail: vi.fn() }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
function setup() {
  const onCreated = vi.fn()
  render(
    <table>
      <tbody>
        <QuickContentForm curriculumId={3} categoryId={8} onCreated={onCreated} onClose={vi.fn()} />
      </tbody>
    </table>,
  )
  return onCreated
}
it('creates consecutive rows with null optional values and restores name focus', async () => {
  const user = userEvent.setup(), onCreated = setup()
  vi.mocked(createContentDetail).mockImplementation(async (_c, _k, body) => ({ ...body, contentDetailId: 1, displayOrder: 1, sheet: null, audio: null }))
  expect(screen.queryByRole('alert')).toBeNull()
  await user.type(screen.getByLabelText('이름'), '싱글')
  await user.click(screen.getByRole('button', { name: '추가' }))
  await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1))
  expect(createContentDetail).toHaveBeenCalledWith(3, 8, { name: '싱글', targetBpm: null, memo: null, evaluationMemo: null, youtubeUrl: null })
  expect(screen.getByLabelText('YouTube URL').getAttribute('type')).toBe('url')
  expect(document.activeElement).toBe(screen.getByLabelText('이름'))
  expect((screen.getByLabelText('이름') as HTMLInputElement).value).toBe('')
  await user.type(screen.getByLabelText('이름'), '더블')
  await user.type(screen.getByLabelText('목표 BPM'), '240')
  await user.type(screen.getByLabelText('YouTube URL'), '  https://youtu.be/abcdefghijk')
  expect((screen.getByLabelText('YouTube URL') as HTMLInputElement).value).toBe('https://youtu.be/abcdefghijk')
  await user.click(screen.getByRole('button', { name: '추가' }))
  await waitFor(() => expect(createContentDetail).toHaveBeenLastCalledWith(3, 8, {
    name: '더블',
    targetBpm: 240,
    memo: null,
    evaluationMemo: null,
    youtubeUrl: 'https://youtu.be/abcdefghijk',
  }))
  await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(2))
})
it('blocks invalid BPM and keeps the row draft when the server rejects a create', async () => {
  const user = userEvent.setup(); setup()
  vi.mocked(createContentDetail).mockRejectedValue(new Error('network'))
  await user.type(screen.getByLabelText('이름'), '싱글')
  await user.type(screen.getByLabelText('목표 BPM'), '59')
  await user.click(screen.getByRole('button', { name: '추가' }))
  expect(createContentDetail).not.toHaveBeenCalled()
  expect(screen.getByRole('alert').textContent).toContain('60 이상 240 이하')
  await user.clear(screen.getByLabelText('목표 BPM'))
  await user.type(screen.getByLabelText('목표 BPM'), '60')
  await user.click(screen.getByRole('button', { name: '추가' }))
  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('요청을 처리하지 못했습니다'))
  expect((screen.getByLabelText('이름') as HTMLInputElement).value).toBe('싱글')
  expect((screen.getByLabelText('목표 BPM') as HTMLInputElement).value).toBe('60')
})
