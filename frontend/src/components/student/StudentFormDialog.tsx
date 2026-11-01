import { useId, useState } from 'react'
import { Overlay } from '../layout/Overlay.tsx'
import type { FormEvent } from 'react'
import { fieldErrorMessage, formErrorMessage } from '../feedback/describeError.ts'
import type { StudentCreateBody, StudentSummary, StudentUpdateBody } from '../../types/student.ts'

type Props = {
  mode: 'create' | 'edit'
  student: StudentSummary | null
  submitting: boolean
  error: unknown
  onClose: () => void
  onCreate: (body: StudentCreateBody) => void
  onUpdate: (studentId: number, body: StudentUpdateBody) => void
}

export function StudentFormDialog({ mode, student, submitting, error, onClose, onCreate, onUpdate }: Props) {
  const [name, setName] = useState(student?.name ?? '')
  const [phone, setPhone] = useState(student?.phone ?? '')
  const [localError, setLocalError] = useState('')

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedName = name.trim()
    if (trimmedName === '') {
      setLocalError('이름은 필수입니다.')
      return
    }
    setLocalError('')
    const body = { name: trimmedName, email: mode === 'edit' ? student?.email ?? null : null, phone: emptyToNull(phone) }
    if (mode === 'create') {
      onCreate(body)
      return
    }
    if (student) {
      onUpdate(student.studentId, body)
    }
  }

  const formId = useId()
  const [discard, setDiscard] = useState(false)
  const dirty = name !== (student?.name ?? '') || phone !== (student?.phone ?? '')
  function requestClose() { if (!submitting) { if (dirty) setDiscard(true); else onClose() } }
  const shown = error ?? (localError ? localError : null)

  return (
    <Overlay title={mode === 'create' ? '학생 추가' : '학생 수정'} onClose={requestClose} className="student-form-overlay">
      <form id={formId} className="overlay-body stack" noValidate onSubmit={onSubmit}>
        <label htmlFor="student-name">이름</label>
        <input aria-invalid={Boolean(localError || fieldErrorMessage(shown, 'name'))} aria-describedby={localError || fieldErrorMessage(shown, 'name') ? 'student-name-error' : undefined} id="student-name" value={name} onChange={(event) => setName(event.target.value)} />
        <FieldMessage id="student-name-error" message={typeof shown === 'string' ? shown : fieldErrorMessage(shown, 'name')} />
        <label htmlFor="student-phone">전화번호</label>
        <input type="tel" autoComplete="tel" aria-describedby={fieldErrorMessage(shown, 'phone') ? 'student-phone-error' : undefined} id="student-phone" value={phone} onChange={(event) => setPhone(event.target.value)} />
        <FieldMessage id="student-phone-error" message={fieldErrorMessage(shown, 'phone')} />
        {shown && typeof shown !== 'string' ? <p className="form-error">{formErrorMessage(shown)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={requestClose} disabled={submitting}>
            취소
          </button>
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? '저장 중' : '저장'}
          </button>
        </div>
      </form>
      {discard ? <Overlay title="입력을 버릴까요?" onClose={() => setDiscard(false)}><div className="overlay-body">
        <p>저장하지 않은 입력이 사라집니다.</p><div className="row-actions">
          <button type="button" className="button" onClick={() => setDiscard(false)}>계속 입력</button>
          <button type="button" className="button button-danger" onClick={onClose}>입력 버리기</button>
        </div></div></Overlay> : null}
    </Overlay>
  )
}

function FieldMessage({ message, id }: { message: string | null; id: string }) {
  if (!message) {
    return null
  }
  return <p id={id} className="field-error" role="alert">{message}</p>
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
