import { lazy, Suspense } from 'react'
import { Link, Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext.tsx'
import { RequireTeacherAuth } from '../auth/RequireTeacherAuth.tsx'
import { AppLayout } from '../components/layout/AppLayout.tsx'
import { CurriculumListPage } from '../pages/CurriculumListPage.tsx'
import { NotFoundPage } from '../pages/NotFoundPage.tsx'
import { LoginPage } from '../pages/LoginPage.tsx'
import { SignupPage } from '../pages/SignupPage.tsx'
import { LocationListPage } from '../pages/LocationListPage.tsx'
import { StudentListPage } from '../pages/StudentListPage.tsx'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/student/*" element={<StudentPaused />} />
      <Route element={<TeacherRoutes />}>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route element={<RequireTeacherAuth />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<Navigate to="/students" replace />} />
            <Route path="/students" element={<StudentListPage />} />
            <Route path="/students/:studentId" element={<Suspense fallback={<p role="status">화면을 불러오는 중</p>}><StudentDetailPage /></Suspense>} />
            <Route path="/locations" element={<LocationListPage />} />
            <Route path="/curriculums" element={<CurriculumListPage />} />
            <Route path="/curriculums/:curriculumId" element={<Suspense fallback={<p role="status">화면을 불러오는 중</p>}><CurriculumBuilderPage /></Suspense>} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

function TeacherRoutes() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}

function StudentPaused() {
  return <main className="login-screen"><section className="login-card">
    <p className="brand">LessonPT</p><h1>학생 서비스 운영 중단</h1>
    <p>학생 전용 서비스는 잠정 중단되었습니다. 수업 관련 문의는 담당 선생님께 해 주세요.</p>
    <Link className="button" to="/login">교사 로그인</Link>
  </section></main>
}

const StudentDetailPage = lazy(() => import('../pages/StudentDetailPage.tsx').then(module => ({ default: module.StudentDetailPage })))

const CurriculumBuilderPage = lazy(() => import('../pages/CurriculumBuilderPage.tsx').then(module => ({ default: module.CurriculumBuilderPage })))
