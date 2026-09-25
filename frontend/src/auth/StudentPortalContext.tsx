import { createContext, useContext, useMemo, useRef, useState, type ReactNode } from 'react'

export type StudentAuthStatus = 'UNAUTHENTICATED' | 'AUTHENTICATED_NO_SCOPE' | 'AUTHENTICATED_SCOPED'

type StudentPortalContextValue = {
  status: StudentAuthStatus
  setStatus: (status: StudentAuthStatus) => void
  rememberAccessKey: (publicAccessKey: string) => void
  readAccessKey: () => string | null
}

const StudentPortalContext = createContext<StudentPortalContextValue | null>(null)

export function StudentPortalProvider({ children }: { children: ReactNode }) {
  const accessKeyRef = useRef<string | null>(null)
  const [status, setStatus] = useState<StudentAuthStatus>('UNAUTHENTICATED')
  const value = useMemo<StudentPortalContextValue>(
    () => ({
      status,
      setStatus,
      rememberAccessKey(publicAccessKey: string) {
        accessKeyRef.current = publicAccessKey
      },
      readAccessKey() {
        return accessKeyRef.current
      },
    }),
    [status],
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
