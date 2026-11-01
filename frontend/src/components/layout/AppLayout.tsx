import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.tsx'
import { Brand } from './Brand.tsx'
import { Icon } from './Icon.tsx'

const links = [
  { to: '/students', label: '학생', icon: 'students' as const },
  { to: '/locations', label: '출강처', icon: 'locations' as const },
  { to: '/curriculums', label: '커리큘럼', icon: 'curriculums' as const },
]
export function AppLayout() {
  const { teacher, logout } = useAuth()
  const location = useLocation()
  const current = links.find(link => location.pathname.startsWith(link.to))
  return <div className="shell">
    <a className="skip-link" href="#main-content">본문으로 이동</a>
    <aside className="sidebar">
      <div className="sidebar-brand"><Brand /><p className="brand-note">선생님을 위한 레슨 워크스페이스</p></div>
      <p className="nav-caption">워크스페이스</p>
      <nav aria-label="주요 메뉴">{links.map(link => <NavLink key={link.to} to={link.to} className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}><Icon name={link.icon}/><span>{link.label}</span></NavLink>)}</nav>
      <div className="sidebar-note"><span className="sidebar-note-rule"/><p>좋은 수업의 흐름을<br/>이어가는 공간.</p><span>LESSONPT · TEACHER WORKSPACE</span></div>
    </aside>
    <div className="workspace">
      <header className="topbar">
        <div className="mobile-brand"><Brand /></div>
        <p className="workspace-path">워크스페이스 <span aria-hidden="true">/</span> <strong>{current?.label ?? '수업 관리'}</strong></p>
        <div className="account-area"><span className="account-avatar" aria-hidden="true">{teacher?.name?.slice(0,1) ?? 'T'}</span><div><p className="teacher-name">{teacher?.name ?? '강사'}<span className="teacher-role">선생님</span></p><p className="teacher-email">{teacher?.email}</p></div><button type="button" className="logout-button" onClick={() => void logout()}><Icon name="logout"/><span>로그아웃</span></button></div>
      </header>
      <main className="content" id="main-content" tabIndex={-1}><Outlet /></main>
    </div>
  </div>
}
