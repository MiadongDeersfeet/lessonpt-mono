import { useState } from 'react'
import type { FormEvent } from 'react'
import { fieldErrorMessage, formErrorMessage } from '../feedback/describeError.ts'
import type { Location, LocationWriteBody } from '../../types/location.ts'

type Props = {
  mode: 'create' | 'edit'
  location: Location | null
  submitting: boolean
  error: unknown
  onClose: () => void
  onCreate: (body: LocationWriteBody) => void
  onUpdate: (locationId: number, body: LocationWriteBody) => void
}

export function LocationFormDialog({ mode, location, submitting, error, onClose, onCreate, onUpdate }: Props) {
  const [name, setName] = useState(location?.name ?? '')
  const [address, setAddress] = useState(location?.address ?? '')
  const [localError, setLocalError] = useState('')

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedName = name.trim()
    if (trimmedName === '') {
      setLocalError('이름은 필수입니다.')
      return
    }
    setLocalError('')
    const body = { name: trimmedName, address: emptyToNull(address) }
    if (mode === 'create') {
      onCreate(body)
      return
    }
    if (location) {
      onUpdate(location.locationId, body)
    }
  }

  const shown = error ?? (localError ? localError : null)

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <form
        className="modal"
        role="dialog"
        aria-labelledby="location-form-title"
        noValidate
        onMouseDown={(event) => event.stopPropagation()}
        onSubmit={onSubmit}
      >
        <h2 id="location-form-title">{mode === 'create' ? '출강처 추가' : '출강처 수정'}</h2>
        <label htmlFor="location-name">이름</label>
        <input id="location-name" value={name} onChange={(event) => setName(event.target.value)} />
        <FieldMessage message={typeof shown === 'string' ? shown : fieldErrorMessage(shown, 'name')} />
        <label htmlFor="location-address">주소</label>
        <input id="location-address" value={address} onChange={(event) => setAddress(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(shown, 'address')} />
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
    </div>
  )
}

function FieldMessage({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return <p className="field-error">{message}</p>
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
