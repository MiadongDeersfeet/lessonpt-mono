import { useState } from 'react'
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
  const [email, setEmail] = useState(student?.email ?? '')
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
    const body = { name: trimmedName, email: emptyToNull(email), phone: emptyToNull(phone) }
    if (mode === 'create') {
      onCreate(body)
      return
    }
    if (student) {
      onUpdate(student.studentId, body)
    }
  }

  const shown = error ?? (localError ? localError : null)

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <form
        className="modal"
        role="dialog"
        aria-labelledby="student-form-title"
        noValidate
        onMouseDown={(event) => event.stopPropagation()}
        onSubmit={onSubmit}
      >
        <h2 id="student-form-title">{mode === 'create' ? '학생 추가' : '학생 수정'}</h2>
        <label htmlFor="student-name">이름</label>
        <input id="student-name" value={name} onChange={(event) => setName(event.target.value)} />
        <FieldMessage message={typeof shown === 'string' ? shown : fieldErrorMessage(shown, 'name')} />
        <label htmlFor="student-email">이메일</label>
        <input id="student-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(shown, 'email')} />
        <label htmlFor="student-phone">전화번호</label>
        <input id="student-phone" value={phone} onChange={(event) => setPhone(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(shown, 'phone')} />
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
