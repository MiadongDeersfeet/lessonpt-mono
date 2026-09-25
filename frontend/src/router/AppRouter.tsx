import { Route, Routes } from 'react-router-dom'
import { RequireTeacherAuth } from '../auth/RequireTeacherAuth.tsx'
import { AppLayout } from '../components/layout/AppLayout.tsx'
import { CurriculumBuilderPage } from '../pages/CurriculumBuilderPage.tsx'
import { CurriculumListPage } from '../pages/CurriculumListPage.tsx'
import { DashboardPage } from '../pages/DashboardPage.tsx'
import { LoginPage } from '../pages/LoginPage.tsx'
import { LocationListPage } from '../pages/LocationListPage.tsx'
import { StudentDetailPage } from '../pages/StudentDetailPage.tsx'
import { StudentListPage } from '../pages/StudentListPage.tsx'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireTeacherAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/students" element={<StudentListPage />} />
          <Route path="/students/:studentId" element={<StudentDetailPage />} />
          <Route path="/locations" element={<LocationListPage />} />
          <Route path="/curriculums" element={<CurriculumListPage />} />
          <Route path="/curriculums/:curriculumId" element={<CurriculumBuilderPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
