import { BrowserRouter } from 'react-router-dom'
import { StudentPortalProvider } from './auth/StudentPortalContext.tsx'
import { AppRouter } from './router/AppRouter.tsx'

export default function App() {
  return (
    <BrowserRouter>
      <StudentPortalProvider>
        <AppRouter />
      </StudentPortalProvider>
    </BrowserRouter>
  )
}
