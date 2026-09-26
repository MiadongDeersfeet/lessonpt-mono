import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { ResizableTable } from './ResizableTable.tsx'
const columns = [{ key: 'name', label: '내용', width: 200, min: 100 }]
function table(key = 'test') { return <ResizableTable columns={columns} storageKey={key} label="내용"><tr><td>싱글</td></tr></ResizableTable> }
afterEach(() => { cleanup(); localStorage.clear(); vi.restoreAllMocks() })
it('persists resized widths across mounts and resets to the default', () => {
  const view = render(table())
  fireEvent.keyDown(screen.getByRole('separator'), { key: 'ArrowRight' })
  expect(screen.getByRole('separator').getAttribute('aria-valuenow')).toBe('216')
  view.unmount(); render(table())
  expect(screen.getByRole('separator').getAttribute('aria-valuenow')).toBe('216')
  fireEvent.click(screen.getByRole('button', { name: '열 설정' }))
  fireEvent.click(screen.getByRole('menuitem', { name: '컬럼 너비 초기화' }))
  expect(screen.getByRole('separator').getAttribute('aria-valuenow')).toBe('200')
  expect(screen.queryByText('컬럼 경계를 드래그해 너비 조절')).toBeNull()
  const help = screen.getByRole('button', { name: '열 너비 안내' })
  expect(document.getElementById(help.getAttribute('aria-describedby') ?? '')?.textContent).toContain('컬럼 경계를 드래그해 너비를 조절할 수 있습니다.')
  expect(screen.getByRole('separator').className).toContain('column-resize')
})
it('keeps builder widths separate from portal widths and shares widths within a view', () => {
  render(<>{table('builder')}{table('builder')}{table('portal')}</>)
  fireEvent.keyDown(screen.getAllByRole('separator')[0], { key: 'ArrowRight' })
  expect(screen.getAllByRole('separator').map(item => item.getAttribute('aria-valuenow'))).toEqual(['216', '216', '200'])
})
it('recovers from invalid storage and respects minimum widths', () => {
  localStorage.setItem('lessonpt.ui.columns.v1.test', 'broken')
  render(table())
  for (let i = 0; i < 20; i++) fireEvent.keyDown(screen.getByRole('separator'), { key: 'ArrowLeft' })
  expect(screen.getByRole('separator').getAttribute('aria-valuenow')).toBe('100')
})
it('continues resizing when browser storage is unavailable', () => {
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('blocked') })
  render(table())
  fireEvent.keyDown(screen.getByRole('separator'), { key: 'ArrowRight' })
  expect(screen.getByRole('separator').getAttribute('aria-valuenow')).toBe('216')
})
