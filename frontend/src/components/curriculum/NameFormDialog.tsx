import { Overlay } from '../layout/Overlay.tsx'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { fieldErrorMessage, formErrorMessage } from '../feedback/describeError.ts'

type Props = {
  title: string
  initialName: string
  submitting: boolean
  error: unknown
  onClose: () => void
  onSubmit: (name: string) => void
}

export function NameFormDialog({ title, initialName, submitting, error, onClose, onSubmit }: Props) {
  const [name, setName] = useState(initialName)
  const [localError, setLocalError] = useState('')

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed === '') {
      setLocalError('이름은 필수입니다.')
      return
    }
    setLocalError('')
    onSubmit(trimmed)
  }

  const shown = error ?? (localError ? localError : null)

  return (
    <Overlay title={title} onClose={() => { if (!submitting) onClose() }} className="form-overlay">
      <form className="overlay-body stack" noValidate onSubmit={submit}>
        <label htmlFor="record-name">이름</label>
        <input id="record-name" value={name} onChange={(event) => setName(event.target.value)} />
        <FieldMessage message={typeof shown === 'string' ? shown : fieldErrorMessage(shown, 'name')} />
        {shown && typeof shown !== 'string' ? <p className="form-error">{formErrorMessage(shown)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? '저장 중' : '저장'}
          </button>
        </div>
      </form>
    </Overlay>
  )
}

function FieldMessage({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return <p className="field-error">{message}</p>
}
