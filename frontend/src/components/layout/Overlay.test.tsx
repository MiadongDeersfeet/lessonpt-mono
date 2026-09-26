import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { Overlay } from './Overlay.tsx'
afterEach(cleanup)
it('closes on Escape and restores focus and body scrolling', () => {
  const close = vi.fn()
  const button = document.createElement('button'); document.body.append(button); button.focus()
  const view = render(<Overlay title="자료" onClose={close}><button>재생</button></Overlay>)
  expect(document.body.style.overflow).toBe('hidden')
  expect(screen.getByRole('dialog').getAttribute('aria-modal')).toBe('true')
  fireEvent.keyDown(document, { key: 'Escape' })
  expect(close).toHaveBeenCalledTimes(1)
  view.unmount()
  expect(document.activeElement).toBe(button)
  expect(document.body.style.overflow).toBe('')
  button.remove()
})
