import { Navigate, Outlet } from 'react-router-dom'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { useAuth } from './AuthContext.tsx'

export function RequireTeacherAuth() {
  const { status } = useAuth()
  if (status === 'loading') {
    return <LoadingState label="계정 확인 중" />
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}
