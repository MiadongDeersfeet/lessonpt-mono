import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { requestStudentLoginOtp } from '../api/studentPortalApi.ts'
import { fieldErrorMessage } from '../components/feedback/describeError.ts'
import { StudentFrame, studentAuthMessage } from './StudentAccessPage.tsx'

export function StudentLoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    const trimmed = email.trim()
    if (trimmed === '') {
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await requestStudentLoginOtp(trimmed)
      navigate('/student/verify', { state: { flow: 'login', email: trimmed } })
    } catch (caught) {
      setError(caught)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <StudentFrame>
      <h1>학생 로그인</h1>
      <p className="lead">등록된 이메일로 인증번호를 받습니다.</p>
      <form className="stack" onSubmit={(event) => void onSubmit(event)}>
        <label htmlFor="student-login-email">이메일</label>
        <input
          id="student-login-email"
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
    </StudentFrame>
  )
}
