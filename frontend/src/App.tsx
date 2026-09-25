import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext.tsx'
import { StudentPortalProvider } from './auth/StudentPortalContext.tsx'
import { AppRouter } from './router/AppRouter.tsx'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <StudentPortalProvider>
          <AppRouter />
        </StudentPortalProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
