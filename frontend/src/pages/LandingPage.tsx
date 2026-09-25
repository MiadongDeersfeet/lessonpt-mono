import { Link } from 'react-router-dom'

export function LandingPage() {
  return (
    <main className="student-portal">
      <p className="brand">LessonPT</p>
      <h1>LessonPT</h1>
      <div className="landing-choices">
        <Link className="card landing-choice" to="/login">
          <strong>선생님으로 시작하기</strong>
          <span>학생 · 커리큘럼 · 진도 · 과제 관리</span>
        </Link>
        <Link className="card landing-choice" to="/student/login">
          <strong>학생으로 시작하기</strong>
          <span>내 수업 · 진도 · 과제 확인</span>
        </Link>
      </div>
    </main>
  )
}
