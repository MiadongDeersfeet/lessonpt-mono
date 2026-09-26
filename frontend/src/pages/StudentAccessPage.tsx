import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { requestStudentOtp } from '../api/studentPortalApi.ts'
import { useStudentPortal } from '../auth/StudentPortalContext.tsx'
import { fieldErrorMessage, formErrorMessage } from '../components/feedback/describeError.ts'

export function StudentAccessPage() {
  const { publicAccessKey } = useParams()
  const navigate = useNavigate()
  const portal = useStudentPortal()
  const [email, setEmail] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!publicAccessKey) {
    return (
      <StudentFrame>
        <h1>학생 접속</h1>
        <p>받은 접속 링크를 다시 열어 주세요.</p>
        <Link to="/student/login">일반 학생 로그인</Link>
      </StudentFrame>
    )
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!publicAccessKey) {
      return
    }
    const trimmed = email.trim()
    if (trimmed === '') {
      return
    }
    portal.rememberAccessKey(publicAccessKey)
    setSubmitting(true)
    setError(null)
    try {
      await requestStudentOtp(publicAccessKey, trimmed)
      navigate('/student/verify', { state: { publicAccessKey, email: trimmed } })
    } catch (caught) {
      setError(caught)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <StudentFrame>
      <h1>학생 접속</h1>
      <p className="lead">등록된 이메일로 인증번호를 받습니다.</p>
      <form className="stack" onSubmit={(event) => void onSubmit(event)}>
        <label htmlFor="student-email">이메일</label>
        <input
          id="student-email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        {fieldErrorMessage(error, 'email') ? <p className="field-error">{fieldErrorMessage(error, 'email')}</p> : null}
        {error ? <p className="form-error">{studentAuthMessage(error)}</p> : null}
        <button type="submit" className="button" disabled={submitting || email.trim() === ''}>
          {submitting ? '요청 중' : '인증번호 받기'}
        </button>
      </form>
      <p className="quiet">
        <Link to="/login">강사 로그인</Link>
      </p>
    </StudentFrame>
  )
}

export function studentAuthMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) {
    return '인증에 실패했습니다.'
  }
  return formErrorMessage(error) ?? '요청을 처리하지 못했습니다.'
}

export function StudentFrame({ children, wide = false }: { children: React.ReactNode; wide?: boolean }) {
  return (
    <main className={`student-portal${wide ? ' student-portal-wide' : ''}`}>
      <p className="brand">LessonPT</p>
      <section className="card student-card">{children}</section>
    </main>
  )
}
