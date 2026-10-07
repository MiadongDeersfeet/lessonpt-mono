import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { signup as signupRequest } from '../api/authApi.ts'
import { useAuth } from '../auth/AuthContext.tsx'
import { teacherPasswordMessage } from '../auth/teacherPassword.ts'

type SignupPhase = 'idle' | 'submitting' | 'invalid' | 'conflict' | 'server'

export function SignupPage() {
  const { status, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [phase, setPhase] = useState<SignupPhase>('idle')
  const [notice, setNotice] = useState('')

  if (status === 'authenticated') {
    return <Navigate to="/students" replace />
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedEmail = email.trim()
    const trimmedName = name.trim()
    if (trimmedEmail === '' || trimmedName === '' || password === '') {
      setPhase('invalid')
      setNotice('이메일, 이름, 비밀번호를 입력해 주세요.')
      return
    }
    const passwordError = teacherPasswordMessage(password)
    if (passwordError) {
      setPhase('invalid')
      setNotice(passwordError)
      return
    }
    if (password !== passwordConfirm) {
      setPhase('invalid')
      setNotice('비밀번호 확인이 일치하지 않습니다.')
      return
    }
    setPhase('submitting')
    setNotice('')
    try {
      await signupRequest({ email: trimmedEmail, password, name: trimmedName })
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setPhase('conflict')
        setNotice(error.message || '이미 사용 중인 이메일입니다.')
        return
      }
      if (error instanceof ApiError && error.status === 400) {
        setPhase('invalid')
        setNotice(error.fieldErrors[0]?.message || '입력값을 확인해 주세요.')
        return
      }
      setPhase('server')
      setNotice('회원가입 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.')
      return
    }
    try {
      await login(trimmedEmail, password)
    } catch {
      navigate('/login', { replace: true, state: { notice: '가입은 완료됐습니다. 로그인해 주세요.' } })
    }
  }

  return (
    <main className="login-screen">
      <form className="login-card" onSubmit={(event) => void onSubmit(event)}>
        <p className="brand">LessonPT</p>
        <h1>강사 회원가입</h1>
        <p className="lead">계정만 만들면 바로 로그인됩니다.</p>
        <label htmlFor="signup-email">이메일</label>
        <input
          id="signup-email"
          name="email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <label htmlFor="signup-name">이름</label>
        <input
          id="signup-name"
          name="name"
          type="text"
          autoComplete="name"
          value={name}
          onChange={(event) => setName(event.target.value)}
        />
        <label htmlFor="signup-password">비밀번호</label>
        <input
          id="signup-password"
          name="password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <label htmlFor="signup-password-confirm">비밀번호 확인</label>
        <input
          id="signup-password-confirm"
          name="passwordConfirm"
          type="password"
          autoComplete="new-password"
          value={passwordConfirm}
          onChange={(event) => setPasswordConfirm(event.target.value)}
        />
        {notice ? (
          <p className={phase === 'server' || phase === 'conflict' ? 'form-error' : 'form-hint'} role="alert">
            {notice}
          </p>
        ) : null}
        <button type="submit" className="button" disabled={phase === 'submitting' || status === 'loading'}>
          {phase === 'submitting' ? '가입 중' : '회원가입'}
        </button>
        <p className="auth-switch">
          이미 계정이 있나요? <Link to="/login">로그인</Link>
        </p>
      </form>
    </main>
  )
}
