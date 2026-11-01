import { AuthLayout } from '../components/layout/AuthLayout.tsx'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { useAuth } from '../auth/AuthContext.tsx'

type LoginPhase = 'idle' | 'submitting' | 'invalid' | 'auth-failed' | 'server'
type LoginLocationState = { notice?: string }

export function LoginPage() {
  const { status, login } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [phase, setPhase] = useState<LoginPhase>('idle')
  const [notice, setNotice] = useState(() => (location.state as LoginLocationState | null)?.notice ?? '')

  if (status === 'authenticated') {
    return <Navigate to="/students" replace />
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (email.trim() === '' || password.trim() === '') {
      setPhase('invalid')
      setNotice('이메일과 비밀번호를 입력해 주세요.')
      return
    }
    setPhase('submitting')
    setNotice('')
    try {
      await login(email.trim(), password)
    } catch (error) {
      if (error instanceof ApiError && (error.status === 400 || error.status === 401)) {
        setPhase(error.status === 401 ? 'auth-failed' : 'invalid')
        setNotice(error.status === 401 ? '이메일 또는 비밀번호가 올바르지 않습니다.' : '입력값을 확인해 주세요.')
        return
      }
      setPhase('server')
      setNotice('로그인 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.')
    }
  }

  return (
    <AuthLayout>
      <form className="login-card" onSubmit={(event) => void onSubmit(event)}>
        <p className="eyebrow">TEACHER WORKSPACE</p>
        <h1>강사 로그인</h1><p className="auth-intro">다시 만나 반갑습니다.<br/>오늘의 수업을 이어가 볼까요?</p>
        <label htmlFor="email">이메일</label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <label htmlFor="password">비밀번호</label>
        <div className="password-input-wrap">
        <input
          id="password"
          name="password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
          <button type="button" className="password-toggle" aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'} aria-pressed={showPassword} onClick={() => setShowPassword(value => !value)}>{showPassword ? '숨김' : '표시'}</button>
        </div>
        {notice ? (
          <p className={phase === 'server' || phase === 'auth-failed' ? 'form-error' : 'form-hint'} role="alert">
            {notice}
          </p>
        ) : null}
        <button type="submit" className="button" disabled={phase === 'submitting' || status === 'loading'}>
          {phase === 'submitting' ? '로그인 중' : '로그인'}
        </button>
        <p className="auth-switch">
          계정이 없나요? <Link to="/signup">회원가입</Link>
        </p>
      </form>
    </AuthLayout>
  )
}
