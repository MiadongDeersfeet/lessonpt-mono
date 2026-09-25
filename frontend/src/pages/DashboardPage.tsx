import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.tsx'

export function DashboardPage() {
  const { teacher } = useAuth()
  return (
    <section className="page">
      <h1>대시보드</h1>
      <p className="lead">{teacher?.name ?? '강사'}님, 학생 학습 현황은 학생 목록에서 확인합니다.</p>
      <Link className="button" to="/students">
        학생 목록
      </Link>
    </section>
  )
}
