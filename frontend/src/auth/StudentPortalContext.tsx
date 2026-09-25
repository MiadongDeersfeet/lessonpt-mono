import { createContext, useContext, useMemo, useRef, type ReactNode } from 'react'

type StudentPortalContextValue = {
  rememberAccessKey: (publicAccessKey: string) => void
  readAccessKey: () => string | null
}

const StudentPortalContext = createContext<StudentPortalContextValue | null>(null)

export function StudentPortalProvider({ children }: { children: ReactNode }) {
  const accessKeyRef = useRef<string | null>(null)
  const value = useMemo<StudentPortalContextValue>(
    () => ({
      rememberAccessKey(publicAccessKey: string) {
        accessKeyRef.current = publicAccessKey
      },
      readAccessKey() {
        return accessKeyRef.current
      },
    }),
    [],
  )
  return <StudentPortalContext.Provider value={value}>{children}</StudentPortalContext.Provider>
}

export function useStudentPortal(): StudentPortalContextValue {
  const value = useContext(StudentPortalContext)
  if (!value) {
    throw new Error('StudentPortalProvider가 없습니다.')
  }
  return value
}
