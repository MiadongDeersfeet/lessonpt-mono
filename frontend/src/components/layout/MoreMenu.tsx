import { useEffect, useId, useRef, useState } from 'react'

type Item = {
  label: string
  onSelect: () => void
  danger?: boolean
}

export function MoreMenu({ label, items }: { label: string; items: Item[] }) {
  const [open, setOpen] = useState(false)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const menuId = useId()

  useEffect(() => {
    if (!open) {
      return
    }
    menuRef.current?.querySelector('button')?.focus()
    function onKey(event: KeyboardEvent) {
      if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key) && menuRef.current?.contains(document.activeElement)) {
        event.preventDefault()
        const buttons = Array.from(menuRef.current.querySelectorAll<HTMLButtonElement>('button:not(:disabled)'))
        const current = buttons.indexOf(document.activeElement as HTMLButtonElement)
        const next = event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1 : (current + (event.key === 'ArrowDown' ? 1 : -1) + buttons.length) % buttons.length
        buttons[next]?.focus()
      }
      if (event.key === 'Escape') {
        setOpen(false)
        triggerRef.current?.focus()
      }
    }
    function onPointer(event: MouseEvent) {
      const target = event.target
      if (!(target instanceof Node)) {
        return
      }
      if (triggerRef.current?.contains(target) || menuRef.current?.contains(target)) {
        return
      }
      setOpen(false)
    }
    document.addEventListener('keydown', onKey)
    document.addEventListener('mousedown', onPointer)
    return () => {
      document.removeEventListener('keydown', onKey)
      document.removeEventListener('mousedown', onPointer)
    }
  }, [open])

  return (
    <div className="more-menu">
      <button
        ref={triggerRef}
        type="button"
        className="more-menu-trigger"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        aria-label={label}
        onClick={() => setOpen((current) => !current)}
      >
        ⋯
      </button>
      {open ? (
        <div ref={menuRef} id={menuId} className="more-menu-panel" role="menu" aria-label={label}>
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={item.danger ? 'more-menu-item danger' : 'more-menu-item'}
              onClick={() => {
                setOpen(false)
                item.onSelect()
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  )
}
