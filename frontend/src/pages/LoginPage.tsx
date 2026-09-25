import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { useAuth } from '../auth/AuthContext.tsx'

type LoginPhase = 'idle' | 'submitting' | 'invalid' | 'auth-failed' | 'server'

export function LoginPage() {
  const { status, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phase, setPhase] = useState<LoginPhase>('idle')
  const [notice, setNotice] = useState('')

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
    <main className="login-screen">
      <form className="login-card" onSubmit={(event) => void onSubmit(event)}>
        <p className="brand">LessonPT</p>
        <h1>강사 로그인</h1>
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
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        {notice ? (
          <p className={phase === 'server' || phase === 'auth-failed' ? 'form-error' : 'form-hint'} role="alert">
            {notice}
          </p>
        ) : null}
        <button type="submit" className="button" disabled={phase === 'submitting' || status === 'loading'}>
          {phase === 'submitting' ? '로그인 중' : '로그인'}
        </button>
      </form>
    </main>
  )
}
