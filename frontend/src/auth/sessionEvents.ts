type SessionLostListener = () => void

let listener: SessionLostListener | null = null

export function setSessionLostListener(next: SessionLostListener): void {
  listener = next
}

export function notifySessionLost(): void {
  listener?.()
}
