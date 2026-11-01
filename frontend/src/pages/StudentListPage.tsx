import { Icon } from '../components/layout/Icon.tsx'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createStudent, getStudentLearning, listStudents, releaseStudent, updateStudent } from '../api/studentApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { ProgressValue } from '../components/student/ProgressValue.tsx'
import { ReleaseStudentDialog } from '../components/student/ReleaseStudentDialog.tsx'
import { StudentFormDialog } from '../components/student/StudentFormDialog.tsx'
import { textOrDash } from '../student/display.ts'
import { summarizeStudent, type StudentListSummary } from '../student/listSummary.ts'
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
  const [query, setQuery] = useState('')
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
    const queue = [...students]
    async function worker() {
      while (active && queue.length) {
        const student = queue.shift()!
        const learning = await getStudentLearning(student.studentId).catch(() => null)
        if (active) setSummaries(current => ({ ...current, [student.studentId]: summarizeStudent(learning, null) }))
      }
    }
    void Promise.all(Array.from({ length: Math.min(4, queue.length) }, worker))
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

  const filtered = students?.filter(student => {
    const text = `${student.name} ${student.phone ?? ''} ${summaries[student.studentId]?.locations ?? ''}`.toLocaleLowerCase()
    return text.includes(query.trim().toLocaleLowerCase())
  })
  return (
    <section className="page">
      <header className="page-header page-header-row">
        <div><p className="eyebrow">STUDENTS</p><h1>학생</h1><p className="lead">학생의 성장과 다음 수업을 한눈에 확인하세요.</p></div>
        <button
          type="button"
          className="button"
          onClick={() => {
            setFormError(null)
            setForm({ mode: 'create', student: null })
          }}
        >
          <Icon name="plus"/>학생 추가
        </button>
      </header>
      <div className="student-search"><label htmlFor="student-search">학생 찾기</label>
        <div className="search-input-wrap"><Icon name="search"/><input id="student-search" type="search" placeholder="이름, 전화번호, 출강처" value={query} onChange={event => setQuery(event.target.value)} /></div>
        <p className="form-hint" role="status">{students ? `${filtered?.length ?? 0}명 / 전체 ${students.length}명` : '학생을 불러오는 중'}</p>
      </div>
      {students && students.length > 0 && filtered?.length === 0 ? <EmptyState message="검색 결과가 없습니다." /> : null}
      {notice ? <p className="form-hint">{notice}</p> : null}
      {error ? <ErrorState error={error} /> : null}
      {!error && students == null ? <LoadingState label="학생 목록을 불러오는 중" /> : null}
      {students?.length === 0 ? <EmptyState message="등록된 학생이 없습니다." /> : null}
      {filtered && filtered.length > 0 ? (
        <div className="student-list-scroll">
        <table className="data-table student-table">
          <thead>
            <tr>
              <th>학생</th>
              <th>전화번호</th>
              <th>출강처</th>
              <th>커리큘럼</th>
              <th>진행</th>
              <th>메모</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((student) => {
              const summary = summaries[student.studentId]
              return (
              <tr key={student.studentId}>
                <td data-label="학생" className="student-name-cell">
                  <Link className="name-link student-name-link" to={`/students/${student.studentId}`}><span className="student-avatar" aria-hidden="true">{student.name.slice(0,1)}</span><span>{student.name}</span></Link>
                </td>
                <td data-label="전화번호">{textOrDash(student.phone)}</td>
                <td data-label="출강처">{summary?.locations ?? '불러오는 중'}</td>
                <td data-label="커리큘럼">{summary?.curricula ?? '불러오는 중'}</td>
                <td data-label="진행">
                  {summary == null ? '불러오는 중' : summary.progress ? <ProgressValue progress={summary.progress} meter /> : '진행 없음'}
                </td>
                <td data-label="메모">
                  {summary?.hasMemo ? (
                    <span className="memo-indicator" title="메모가 있습니다." aria-label="메모가 있습니다.">● 메모 있음</span>
                  ) : summary ? (
                    <span className="quiet">메모 없음</span>
                  ) : null}
                </td>
                <td data-label="작업" className="row-actions">
                  <Link className="button" to={`/students/${student.studentId}?tab=learning`}>학습관리<Icon name="arrow"/></Link>
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
                    className="button button-quiet button-destructive-quiet"
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
