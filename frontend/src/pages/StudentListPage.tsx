import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createStudent, listStudents, releaseStudent, updateStudent } from '../api/studentApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { ReleaseStudentDialog } from '../components/student/ReleaseStudentDialog.tsx'
import { StudentFormDialog } from '../components/student/StudentFormDialog.tsx'
import { textOrDash } from '../student/display.ts'
import type { StudentCreateBody, StudentSummary, StudentUpdateBody } from '../types/student.ts'

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
      setNotice('학생 연결을 해제했습니다.')
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
        <table className="data-table">
          <thead>
            <tr>
              <th>이름</th>
              <th>이메일</th>
              <th>전화번호</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {students.map((student) => (
              <tr key={student.studentId}>
                <td>
                  <Link to={`/students/${student.studentId}`}>{student.name}</Link>
                </td>
                <td>{textOrDash(student.email)}</td>
                <td>{textOrDash(student.phone)}</td>
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
                    연결 해제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
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
