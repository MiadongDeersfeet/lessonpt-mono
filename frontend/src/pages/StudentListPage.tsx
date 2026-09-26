import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError } from '../api/apiClient.ts'
import { createStudent, getStudentAccess, getStudentLearning, listStudents, releaseStudent, updateStudent } from '../api/studentApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { ReleaseStudentDialog } from '../components/student/ReleaseStudentDialog.tsx'
import { StudentFormDialog } from '../components/student/StudentFormDialog.tsx'
import { textOrDash } from '../student/display.ts'
import { summarizeStudent, type StudentListSummary } from '../student/listSummary.ts'
import type { StudentAccess, StudentCreateBody, StudentSummary, StudentUpdateBody } from '../types/student.ts'

type FormState = { mode: 'create'; student: null } | { mode: 'edit'; student: StudentSummary }

export function StudentListPage() {
  const [students, setStudents] = useState<StudentSummary[] | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [notice, setNotice] = useState('')
  const [form, setForm] = useState<FormState | null>(null)
  const [formError, setFormError] = useState<unknown>(null)
  const [formSubmitting, setFormSubmitting] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<StudentSummary | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releaseSubmitting, setReleaseSubmitting] = useState(false)
  const [summaries, setSummaries] = useState<Record<number, StudentListSummary>>({})

  useEffect(() => {
    let active = true
    listStudents()
      .then((rows) => {
        if (active) {
          setStudents(rows)
        }
      })
      .catch((caught) => {
        if (active) {
          setError(caught)
        }
      })
    return () => {
      active = false
    }
  }, [])

  const studentIds = students?.map((student) => student.studentId).join(',') ?? ''
  useEffect(() => {
    if (!students || students.length === 0) {
      return
    }
    let active = true
    Promise.all(
      students.map(async (student) => {
        const [learning, access] = await Promise.all([
          getStudentLearning(student.studentId).catch(() => null),
          getStudentAccess(student.studentId).catch((error: unknown): StudentAccess | null | 'error' =>
            error instanceof ApiError && error.status === 404 && error.code === 'COMMON_NOT_FOUND' ? null : 'error',
          ),
        ])
        const portalActive = access === 'error' ? null : access?.status === 'ACTIVE'
        return [student.studentId, summarizeStudent(learning, portalActive)] as const
      }),
    ).then((rows) => {
      if (active) {
        setSummaries(Object.fromEntries(rows))
      }
    })
    return () => {
      active = false
    }
  }, [studentIds, students])

  async function onCreate(body: StudentCreateBody) {
    setFormSubmitting(true)
    setFormError(null)
    try {
      const created = await createStudent(body)
      setStudents((current) => (current ? [...current, created] : [created]))
      setForm(null)
      setNotice('학생을 추가했습니다.')
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onUpdate(studentId: number, body: StudentUpdateBody) {
    setFormSubmitting(true)
    setFormError(null)
    try {
      const updated = await updateStudent(studentId, body)
      setStudents((current) => current?.map((row) => (row.studentId === studentId ? updated : row)) ?? [updated])
      setForm(null)
      setNotice('학생 정보를 수정했습니다.')
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onRelease() {
    if (!releaseTarget) {
      return
    }
    setReleaseSubmitting(true)
    setReleaseError(null)
    try {
      await releaseStudent(releaseTarget.studentId)
      setStudents((current) => current?.filter((row) => row.studentId !== releaseTarget.studentId) ?? [])
      setReleaseTarget(null)
      setNotice('학생을 삭제했습니다.')
    } catch (caught) {
      setReleaseError(caught)
    } finally {
      setReleaseSubmitting(false)
    }
  }

  return (
    <section className="page">
      <header className="page-header page-header-row">
        <h1>학생</h1>
        <button
          type="button"
          className="button"
          onClick={() => {
            setFormError(null)
            setForm({ mode: 'create', student: null })
          }}
        >
          학생 추가
        </button>
      </header>
      {notice ? <p className="form-hint">{notice}</p> : null}
      {error ? <ErrorState error={error} /> : null}
      {!error && students == null ? <LoadingState label="학생 목록을 불러오는 중" /> : null}
      {students?.length === 0 ? <EmptyState message="등록된 학생이 없습니다." /> : null}
      {students && students.length > 0 ? (
        <div className="student-list-scroll">
        <table className="data-table">
          <thead>
            <tr>
              <th>학생</th>
              <th>이메일</th>
              <th>전화번호</th>
              <th>출강처</th>
              <th>커리큘럼</th>
              <th>진행</th>
              <th>Portal</th>
              <th>메모</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {students.map((student) => {
              const summary = summaries[student.studentId]
              return (
              <tr key={student.studentId}>
                <td>
                  <Link className="name-link" to={`/students/${student.studentId}`}>{student.name}</Link>
                </td>
                <td>{textOrDash(student.email)}</td>
                <td>{textOrDash(student.phone)}</td>
                <td>{summary?.locations ?? '불러오는 중'}</td>
                <td>{summary?.curricula ?? '불러오는 중'}</td>
                <td>{summary?.progressLabel ?? '불러오는 중'}</td>
                <td>{summary?.portalLabel ?? '불러오는 중'}</td>
                <td>
                  {summary?.hasMemo ? (
                    <span className="memo-indicator" title="메모가 있습니다." aria-label="메모가 있습니다.">● 메모 있음</span>
                  ) : summary ? (
                    <span className="quiet">메모 없음</span>
                  ) : null}
                </td>
                <td className="row-actions">
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setFormError(null)
                      setForm({ mode: 'edit', student })
                    }}
                  >
                    편집
                  </button>
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setReleaseError(null)
                      setReleaseTarget(student)
                    }}
                  >
                    학생 삭제
                  </button>
                </td>
              </tr>
              )
            })}
          </tbody>
        </table>
        </div>
      ) : null}
      {form ? (
        <StudentFormDialog
          mode={form.mode}
          student={form.student}
          submitting={formSubmitting}
          error={formError}
          onClose={() => {
            if (!formSubmitting) {
              setForm(null)
            }
          }}
          onCreate={(body) => void onCreate(body)}
          onUpdate={(studentId, body) => void onUpdate(studentId, body)}
        />
      ) : null}
      {releaseTarget ? (
        <ReleaseStudentDialog
          name={releaseTarget.name}
          submitting={releaseSubmitting}
          error={releaseError}
          onClose={() => {
            if (!releaseSubmitting) {
              setReleaseTarget(null)
            }
          }}
          onConfirm={() => void onRelease()}
        />
      ) : null}
    </section>
  )
}
