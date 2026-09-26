import { useCallback, useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { MoreMenu } from './MoreMenu.tsx'

export type Column = { key: string; label: string; width: number; min?: number }
const changeEvent = 'lessonpt:column-widths'

export function ResizableTable({ columns, storageKey, label, fill = false, children }: {
  columns: Column[]; storageKey: string; label: string; fill?: boolean; children: ReactNode
}) {
  const key = `lessonpt.ui.columns.v1.${storageKey}`
  const read = useCallback((): Record<string, number> => {
    try {
      const saved = JSON.parse(localStorage.getItem(key) ?? '{}')
      return Object.fromEntries(columns.map(c => [c.key, typeof saved?.[c.key] === 'number' && Number.isFinite(saved[c.key]) ? Math.max(c.min ?? 80, Math.min(1200, saved[c.key])) : c.width]))
    } catch { return Object.fromEntries(columns.map(c => [c.key, c.width])) }
  }, [key, columns])
  const [widths, setWidths] = useState(read)
  const drag = useRef<{ key: string; x: number; width: number } | null>(null)
  const helpId = useId()
  useEffect(() => {
    const sync = () => setWidths(read())
    window.addEventListener(changeEvent, sync)
    window.addEventListener('storage', sync)
    return () => { window.removeEventListener(changeEvent, sync); window.removeEventListener('storage', sync) }
  }, [read])
  function save(next: Record<string, number>) {
    setWidths(next)
    try { localStorage.setItem(key, JSON.stringify(next)); window.dispatchEvent(new Event(changeEvent)) } catch { /* Resizing still works when storage is unavailable. */ }
  }
  function resize(column: Column, value: number) {
    save({ ...widths, [column.key]: Math.max(column.min ?? 80, Math.min(1200, value)) })
  }
  return <div className="sheet">
    <div className="sheet-tools"><span className="column-help"><button type="button" className="help-button" aria-label="열 너비 안내" aria-describedby={helpId}>ⓘ</button><span id={helpId} role="tooltip" className="help-tooltip">컬럼 경계를 드래그해 너비를 조절할 수 있습니다. 설정은 이 브라우저에 저장됩니다.</span></span><MoreMenu label="열 설정" items={[{ label: '컬럼 너비 초기화', onSelect: () => save(Object.fromEntries(columns.map(c => [c.key, c.width]))) }]} /></div>
    <div className="sheet-scroll" tabIndex={0} role="region" aria-label={`${label} 목록`}>
      <table className="data-table content-table" aria-label={label} style={{ width: fill ? '100%' : columns.reduce((total, c) => total + (widths[c.key] ?? c.width), 0), minWidth: fill ? columns.reduce((total, c) => total + (widths[c.key] ?? c.width), 0) : undefined }}>
        <colgroup>{columns.map(c => <col key={c.key} style={{ width: widths[c.key] ?? c.width }} />)}</colgroup>
        <thead><tr>{columns.map(c => <th scope="col" key={c.key}>{c.label}<span className="column-resize" role="separator" tabIndex={0} aria-label={`${c.label} 너비`} aria-orientation="vertical" aria-valuemin={c.min ?? 80} aria-valuemax={1200} aria-valuenow={widths[c.key] ?? c.width}
          onKeyDown={event => { if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); resize(c, (widths[c.key] ?? c.width) + (event.key === 'ArrowRight' ? 16 : -16)) } if (event.key === 'Home') { event.preventDefault(); resize(c, c.width) } }}
          onPointerDown={event => { event.preventDefault(); event.currentTarget.focus(); event.currentTarget.setPointerCapture(event.pointerId); drag.current = { key: c.key, x: event.clientX, width: widths[c.key] ?? c.width } }}
          onPointerMove={event => { if (drag.current?.key === c.key) resize(c, drag.current.width + event.clientX - drag.current.x) }}
          onPointerUp={() => { drag.current = null }} onPointerCancel={() => { drag.current = null }} onLostPointerCapture={() => { drag.current = null }}
        /></th>)}</tr></thead><tbody>{children}</tbody>
      </table>
    </div>
  </div>
}
