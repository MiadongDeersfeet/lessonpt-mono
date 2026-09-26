import { useEffect, useRef } from 'react'

export function Toast({ message, onDone }: { message: string; onDone: () => void }) {
  const done = useRef(onDone)
  useEffect(() => {
    done.current = onDone
  })
  useEffect(() => {
    const timer = window.setTimeout(() => done.current(), 2400)
    return () => window.clearTimeout(timer)
  }, [message])
  return (
    <p className="toast" role="status">
      {message}
    </p>
  )
}
