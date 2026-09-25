import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { login as loginRequest, logout as logoutRequest, me } from '../api/authApi.ts'
import { setSessionLostListener } from './sessionEvents.ts'
import { clearTokens, readTokens, saveTokens } from './tokenStore.ts'
import type { TeacherMe } from '../types/auth.ts'

export type AuthStatus = 'loading' | 'anonymous' | 'authenticated'

type AuthState = {
  status: AuthStatus
  teacher: TeacherMe | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [status, setStatus] = useState<AuthStatus>(() => (readTokens() ? 'loading' : 'anonymous'))
  const [teacher, setTeacher] = useState<TeacherMe | null>(null)

  useEffect(() => {
    setSessionLostListener(() => {
      setTeacher(null)
      setStatus('anonymous')
      navigate('/login', { replace: true })
    })
  }, [navigate])

  useEffect(() => {
    if (!readTokens()) {
      return
    }
    let active = true
    me()
      .then((next) => {
        if (!active) {
          return
        }
        setTeacher(next)
        setStatus('authenticated')
      })
      .catch(() => {
        if (!active) {
          return
        }
        clearTokens()
        setTeacher(null)
        setStatus('anonymous')
      })
    return () => {
      active = false
    }
  }, [])

  const value = useMemo<AuthState>(() => {
    return {
      status,
      teacher,
      async login(email: string, password: string) {
        const tokens = await loginRequest({ email, password })
        saveTokens(tokens)
        try {
          const next = await me()
          setTeacher(next)
          setStatus('authenticated')
        } catch (error) {
          clearTokens()
          setTeacher(null)
          setStatus('anonymous')
          throw error
        }
      },
      async logout() {
        try {
          await logoutRequest()
        } catch {
          // 서버 세션 종료가 실패해도 이 브라우저의 토큰은 버린다.
        }
        clearTokens()
        setTeacher(null)
        setStatus('anonymous')
        navigate('/login', { replace: true })
      },
    }
  }, [navigate, status, teacher])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('AuthProvider가 없습니다.')
  }
  return value
}
