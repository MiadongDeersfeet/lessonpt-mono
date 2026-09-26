import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="student-portal">
      <p className="brand">LessonPT</p>
      <section className="page card">
        <p className="lead">404</p>
        <h1>페이지를 찾을 수 없습니다.</h1>
        <p>요청하신 페이지가 존재하지 않거나 이동되었습니다.</p>
        <Link className="button" to="/">
          홈으로 이동
        </Link>
      </section>
    </main>
  )
}
