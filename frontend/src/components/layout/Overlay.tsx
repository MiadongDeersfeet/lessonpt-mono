import { useEffect, useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

const layers: HTMLElement[] = []

export function Overlay({ title, children, onClose, className = '' }: {
  title: string; children: ReactNode; onClose: () => void; className?: string
}) {
  const ref = useRef<HTMLDivElement>(null)
  const close = useRef(onClose)
  const id = useId()
  useEffect(() => { close.current = onClose }, [onClose])
  useEffect(() => {
    const element = ref.current!
    const previous = document.activeElement as HTMLElement | null
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    layers.push(element)
    element.focus()
    function key(event: KeyboardEvent) {
      if (layers.at(-1) !== element) return
      if (event.key === 'Escape') { event.preventDefault(); close.current(); return }
      if (event.key !== 'Tab') return
      const items = Array.from(element.querySelectorAll<HTMLElement>('button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), iframe, audio[controls], [tabindex="0"]')).filter(item => !item.closest('[hidden]'))
      const first = items[0], last = items.at(-1)
      if (!first) { event.preventDefault(); element.focus() }
      else if (event.shiftKey && (document.activeElement === first || document.activeElement === element)) { event.preventDefault(); last?.focus() }
      else if (!event.shiftKey && (document.activeElement === last || document.activeElement === element)) { event.preventDefault(); first.focus() }
    }
    function contain(event: FocusEvent) {
      if (layers.at(-1) === element && !element.contains(event.target as Node)) element.focus()
    }
    document.addEventListener('keydown', key)
    document.addEventListener('focusin', contain)
    return () => {
      layers.splice(layers.indexOf(element), 1)
      document.body.style.overflow = overflow
      document.removeEventListener('keydown', key)
      document.removeEventListener('focusin', contain)
      if (previous?.isConnected) previous.focus()
    }
  }, [])
  return createPortal(
    <div className="overlay-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
      <div ref={ref} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby={id} className={`overlay ${className}`}>
        <header className="overlay-header"><h2 id={id}>{title}</h2><button type="button" className="button button-quiet" onClick={onClose} aria-label="닫기">닫기</button></header>
        {children}
      </div>
    </div>, document.body,
  )
}
