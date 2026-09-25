import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { getStudentMe, listStudentRelationships, selectStudentScope } from '../api/studentPortalApi.ts'
import { useStudentPortal } from '../auth/StudentPortalContext.tsx'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { formErrorMessage } from '../components/feedback/describeError.ts'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import type { StudentRelationship } from '../types/studentPortal.ts'
import { StudentFrame } from './StudentAccessPage.tsx'

export function StudentRelationshipsPage() {
  const navigate = useNavigate()
  const { setStatus } = useStudentPortal()
  const [name, setName] = useState('')
  const [relationships, setRelationships] = useState<StudentRelationship[] | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [selectingId, setSelectingId] = useState<number | null>(null)

  useEffect(() => {
    let active = true
    getStudentMe()
      .then(async (me) => {
        const next = await listStudentRelationships()
        if (!active) {
          return
        }
        setName(me.name)
        setStatus('AUTHENTICATED_NO_SCOPE')
        setRelationships(next)
        if (next.length !== 1) {
          return
        }
        setSelectingId(next[0].teacherStudentAccessId)
        await selectStudentScope(next[0].teacherStudentAccessId)
        if (!active) {
          return
        }
        navigate('/student', { replace: true })
      })
      .catch((caught) => {
        if (!active) {
          return
        }
        setSelectingId(null)
        if (caught instanceof ApiError && caught.status === 401) {
          setStatus('UNAUTHENTICATED')
          navigate('/student/login', { replace: true })
          return
        }
        setError(caught)
      })
    return () => {
      active = false
    }
  }, [navigate, setStatus])

  async function choose(teacherStudentAccessId: number) {
    setSelectingId(teacherStudentAccessId)
    setError(null)
    try {
      await selectStudentScope(teacherStudentAccessId)
      navigate('/student', { replace: true })
    } catch (caught) {
      setSelectingId(null)
      if (caught instanceof ApiError && caught.status === 401) {
        setStatus('UNAUTHENTICATED')
        navigate('/student/login', { replace: true })
        return
      }
      setError(caught)
    }
  }

  return (
    <StudentFrame>
      <h1>{name ? `${name}님, 수업을 선택하세요.` : '수업을 선택하세요.'}</h1>
      {relationships == null && !error ? <LoadingState label="수업 목록을 불러오는 중" /> : null}
      {error ? <p className="form-error">{formErrorMessage(error)}</p> : null}
      {relationships?.length === 0 ? <EmptyState message="접근 가능한 수업이 없습니다." /> : null}
      {relationships && (relationships.length > 1 || (relationships.length === 1 && error))
        ? relationships.map((item) => (
            <article className="card relationship-card" key={item.teacherStudentAccessId}>
              <h2>{item.teacherName} 선생님</h2>
              {item.locations.length === 0 ? <p className="quiet">등록된 출강처 없음</p> : null}
              {item.locations.map((location) => (
                <p key={location.locationId}>{location.locationName}</p>
              ))}
              <button
                type="button"
                className="button"
                disabled={selectingId != null}
                onClick={() => void choose(item.teacherStudentAccessId)}
              >
                수업 보기
              </button>
            </article>
          ))
        : null}
    </StudentFrame>
  )
}
