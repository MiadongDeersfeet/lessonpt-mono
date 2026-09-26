import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createCurriculum, deactivateCurriculum, listCurriculums, updateCurriculum } from '../api/curriculumApi.ts'
import { DeactivateDialog } from '../components/curriculum/DeactivateDialog.tsx'
import { NameFormDialog } from '../components/curriculum/NameFormDialog.tsx'
import { EmptyState } from '../components/feedback/EmptyState.tsx'
import { ErrorState } from '../components/feedback/ErrorState.tsx'
import { LoadingState } from '../components/feedback/LoadingState.tsx'
import type { Curriculum } from '../types/curriculum.ts'

type FormState = { mode: 'create' } | { mode: 'edit'; curriculum: Curriculum }

const deactivateMessage =
  '이 커리큘럼을 삭제합니다. 활성 카테고리와 내용도 함께 삭제됩니다. 학생 수강, 모니터링, 과제 기록은 남고, 커리큘럼을 다시 활성화해도 카테고리와 내용은 자동으로 돌아오지 않습니다.'

export function CurriculumListPage() {
  const [curriculums, setCurriculums] = useState<Curriculum[] | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [notice, setNotice] = useState('')
  const [form, setForm] = useState<FormState | null>(null)
  const [formError, setFormError] = useState<unknown>(null)
  const [formSubmitting, setFormSubmitting] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<Curriculum | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releaseSubmitting, setReleaseSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    listCurriculums()
      .then((rows) => {
        if (active) {
          setCurriculums(rows)
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

  async function onCreate(name: string) {
    setFormSubmitting(true)
    setFormError(null)
    try {
      const created = await createCurriculum(name)
      setCurriculums((current) => insertCurriculum(current, created))
      setForm(null)
      setNotice('커리큘럼을 추가했습니다.')
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onUpdate(curriculumId: number, name: string) {
    setFormSubmitting(true)
    setFormError(null)
    try {
      const updated = await updateCurriculum(curriculumId, name)
      setCurriculums((current) => current?.map((row) => (row.curriculumId === curriculumId ? updated : row)) ?? [updated])
      setForm(null)
      setNotice('커리큘럼 이름을 수정했습니다.')
    } catch (caught) {
      setFormError(caught)
    } finally {
      setFormSubmitting(false)
    }
  }

  async function onDeactivate() {
    if (!releaseTarget) {
      return
    }
    setReleaseSubmitting(true)
    setReleaseError(null)
    try {
      await deactivateCurriculum(releaseTarget.curriculumId)
      setCurriculums(await listCurriculums())
      setReleaseTarget(null)
      setNotice('커리큘럼을 삭제했습니다.')
    } catch (caught) {
      setReleaseError(caught)
    } finally {
      setReleaseSubmitting(false)
    }
  }

  return (
    <section className="page">
      <header className="page-header page-header-row">
        <h1>커리큘럼</h1>
        <button
          type="button"
          className="button"
          onClick={() => {
            setFormError(null)
            setForm({ mode: 'create' })
          }}
        >
          커리큘럼 추가
        </button>
      </header>
      {notice ? <p className="form-hint">{notice}</p> : null}
      {error ? <ErrorState error={error} /> : null}
      {!error && curriculums == null ? <LoadingState label="커리큘럼 목록을 불러오는 중" /> : null}
      {curriculums?.length === 0 ? <EmptyState message="등록된 커리큘럼이 없습니다." /> : null}
      {curriculums && curriculums.length > 0 ? (
        <table className="data-table">
          <thead>
            <tr>
              <th>순서</th>
              <th>이름</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {curriculums.map((curriculum) => (
              <tr key={curriculum.curriculumId}>
                <td>{curriculum.displayOrder}</td>
                <td>
                  <Link className="name-link" to={`/curriculums/${curriculum.curriculumId}`}>{curriculum.name}</Link>
                </td>
                <td className="row-actions">
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setFormError(null)
                      setForm({ mode: 'edit', curriculum })
                    }}
                  >
                    편집
                  </button>
                  <button
                    type="button"
                    className="button button-quiet"
                    onClick={() => {
                      setReleaseError(null)
                      setReleaseTarget(curriculum)
                    }}
                  >
                    커리큘럼 삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
      {form ? (
        <NameFormDialog
          title={form.mode === 'create' ? '커리큘럼 추가' : '커리큘럼 수정'}
          initialName={form.mode === 'edit' ? form.curriculum.name : ''}
          submitting={formSubmitting}
          error={formError}
          onClose={() => {
            if (!formSubmitting) {
              setForm(null)
            }
          }}
          onSubmit={(name) => {
            if (form.mode === 'create') {
              void onCreate(name)
              return
            }
            void onUpdate(form.curriculum.curriculumId, name)
          }}
        />
      ) : null}
      {releaseTarget ? (
        <DeactivateDialog
          title="커리큘럼 삭제"
          confirmLabel="삭제"
          message={deactivateMessage}
          submitting={releaseSubmitting}
          error={releaseError}
          onClose={() => {
            if (!releaseSubmitting) {
              setReleaseTarget(null)
            }
          }}
          onConfirm={() => void onDeactivate()}
        />
      ) : null}
    </section>
  )
}

function insertCurriculum(current: Curriculum[] | null, created: Curriculum): Curriculum[] {
  const rows = (current ?? []).filter((row) => row.curriculumId !== created.curriculumId)
  rows.push(created)
  rows.sort((left, right) => left.displayOrder - right.displayOrder || left.curriculumId - right.curriculumId)
  return rows
}
