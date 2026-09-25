import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.tsx'

const links = [
  { to: '/', label: '대시보드', end: true },
  { to: '/students', label: '학생', end: false },
  { to: '/locations', label: '출강처', end: false },
  { to: '/curriculums', label: '커리큘럼', end: false },
]

export function AppLayout() {
  const { teacher, logout } = useAuth()
  return (
    <div className="shell">
      <aside className="sidebar">
        <p className="brand">LessonPT</p>
        <nav aria-label="주요 메뉴">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className="nav-link">
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div>
            <p className="teacher-name">{teacher?.name ?? '강사'}</p>
            <p className="teacher-email">{teacher?.email}</p>
          </div>
          <button type="button" className="button button-quiet" onClick={() => void logout()}>
            로그아웃
          </button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
