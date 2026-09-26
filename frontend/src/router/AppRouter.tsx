import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext.tsx'
import { RequireTeacherAuth } from '../auth/RequireTeacherAuth.tsx'
import { AppLayout } from '../components/layout/AppLayout.tsx'
import { CurriculumBuilderPage } from '../pages/CurriculumBuilderPage.tsx'
import { CurriculumListPage } from '../pages/CurriculumListPage.tsx'
import { LandingPage } from '../pages/LandingPage.tsx'
import { NotFoundPage } from '../pages/NotFoundPage.tsx'
import { LoginPage } from '../pages/LoginPage.tsx'
import { LocationListPage } from '../pages/LocationListPage.tsx'
import { StudentDetailPage } from '../pages/StudentDetailPage.tsx'
import { StudentAccessPage } from '../pages/StudentAccessPage.tsx'
import { StudentListPage } from '../pages/StudentListPage.tsx'
import { StudentLoginPage } from '../pages/StudentLoginPage.tsx'
import { StudentPortalPage } from '../pages/StudentPortalPage.tsx'
import { StudentRelationshipsPage } from '../pages/StudentRelationshipsPage.tsx'
import { StudentVerifyPage } from '../pages/StudentVerifyPage.tsx'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/student/login" element={<StudentLoginPage />} />
      <Route path="/student/access" element={<StudentAccessPage />} />
      <Route path="/student/access/:publicAccessKey" element={<StudentAccessPage />} />
      <Route path="/student/verify" element={<StudentVerifyPage />} />
      <Route path="/student/relationships" element={<StudentRelationshipsPage />} />
      <Route path="/student" element={<StudentPortalPage />} />
      <Route element={<TeacherRoutes />}>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<RequireTeacherAuth />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<Navigate to="/students" replace />} />
            <Route path="/students" element={<StudentListPage />} />
            <Route path="/students/:studentId" element={<StudentDetailPage />} />
            <Route path="/locations" element={<LocationListPage />} />
            <Route path="/curriculums" element={<CurriculumListPage />} />
            <Route path="/curriculums/:curriculumId" element={<CurriculumBuilderPage />} />
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
