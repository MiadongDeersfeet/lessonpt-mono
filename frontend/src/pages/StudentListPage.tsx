import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listStudents } from '../api/studentApi.ts'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import { textOrDash } from '../student/display.ts'
import type { StudentSummary } from '../types/student.ts'

export function StudentListPage() {
  const [students, setStudents] = useState<StudentSummary[] | null>(null)
  const [error, setError] = useState<unknown>(null)

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

  return (
    <section className="page">
      <h1>학생</h1>
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
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </section>
  )
}
