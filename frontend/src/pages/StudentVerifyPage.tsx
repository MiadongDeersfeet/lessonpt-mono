import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { verifyStudentOtp } from '../api/studentPortalApi.ts'
import { useStudentPortal } from '../auth/StudentPortalContext.tsx'
import { fieldErrorMessage } from '../components/feedback/describeError.ts'
import { StudentFrame, studentAuthMessage } from './StudentAccessPage.tsx'

type VerifyState = {
  publicAccessKey?: string
  email?: string
}

export function StudentVerifyPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const portal = useStudentPortal()
  const state = (location.state ?? {}) as VerifyState
  const publicAccessKey = state.publicAccessKey ?? portal.readAccessKey() ?? ''
  const email = state.email ?? ''
  const [otp, setOtp] = useState('')
  const [otpError, setOtpError] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!publicAccessKey || !email) {
    return (
      <StudentFrame>
        <h1>인증번호 확인</h1>
        <p>접속 링크에서 이메일을 입력한 뒤 인증번호를 요청해 주세요.</p>
        {publicAccessKey ? <Link to={`/student/access/${publicAccessKey}`}>이메일 입력으로</Link> : <Link to="/student/access">접속 안내</Link>}
      </StudentFrame>
    )
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (!/^\d{6}$/.test(otp.trim())) {
      setOtpError('인증번호는 숫자 6자리입니다.')
      return
    }
    setOtpError('')
    setSubmitting(true)
    setError(null)
    try {
      await verifyStudentOtp(publicAccessKey, email, otp.trim())
      navigate('/student', { replace: true })
    } catch (caught) {
      setError(caught)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <StudentFrame>
      <h1>인증번호 확인</h1>
      <p className="lead">{email}</p>
      <form className="stack" onSubmit={(event) => void onSubmit(event)}>
        <label htmlFor="student-otp">인증번호</label>
        <input
          id="student-otp"
          inputMode="numeric"
          autoComplete="one-time-code"
          value={otp}
          onChange={(event) => setOtp(event.target.value)}
        />
        {otpError ? <p className="field-error">{otpError}</p> : null}
        {fieldErrorMessage(error, 'otp') ? <p className="field-error">{fieldErrorMessage(error, 'otp')}</p> : null}
        {error ? <p className="form-error">{studentAuthMessage(error)}</p> : null}
        <button type="submit" className="button" disabled={submitting || otp.trim() === ''}>
          {submitting ? '확인 중' : '확인'}
        </button>
      </form>
    </StudentFrame>
  )
}
