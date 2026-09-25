import { useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import {
  assignStudentCurriculum,
  releaseStudentCurriculum,
  updateStudentCurriculumMemo,
} from '../../api/studentCurriculumApi.ts'
import { formErrorMessage } from '../feedback/describeError.ts'
import { EmptyState } from '../feedback/EmptyState.tsx'
import { ErrorState } from '../feedback/ErrorState.tsx'
import { LoadingState } from '../feedback/LoadingState.tsx'
import type { Curriculum } from '../../types/curriculum.ts'
import type { LearningLocation, StudentCurriculum } from '../../types/student.ts'

type Props = {
  location: LearningLocation
  catalog: Curriculum[] | null
  catalogError: unknown
  onRefreshLearning: () => Promise<void>
}

export function StudentCurriculumPanel({ location, catalog, catalogError, onRefreshLearning }: Props) {
  const [selectedId, setSelectedId] = useState('')
  const [memo, setMemo] = useState('')
  const [notice, setNotice] = useState('')
  const [assignError, setAssignError] = useState<unknown>(null)
  const [assigning, setAssigning] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<StudentCurriculum | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releasing, setReleasing] = useState(false)
  const [memoDrafts, setMemoDrafts] = useState<Record<number, string>>({})
  const [memoError, setMemoError] = useState<unknown>(null)
  const [savingMemoId, setSavingMemoId] = useState<number | null>(null)

  const assignedIds = new Set(location.studentCurriculums.map((item) => item.curriculumId))
  const available = catalog?.filter((item) => !assignedIds.has(item.curriculumId)) ?? []
  const selectId = `assign-curriculum-${location.teacherStudentLocationId}`

  async function onAssign() {
    const curriculumId = Number(selectedId)
    if (!Number.isInteger(curriculumId) || curriculumId <= 0) {
      return
    }
    setAssigning(true)
    setAssignError(null)
    setNotice('')
    try {
      const assigned = await assignStudentCurriculum(location.teacherStudentLocationId, {
        curriculumId,
        memo: emptyToNull(memo),
      })
      await onRefreshLearning()
      setSelectedId('')
      setMemo('')
      setNotice(
        assigned.reenrolled
          ? '기존 커리큘럼 배정을 다시 활성화했습니다. 다시 활성화할 때는 입력한 메모가 저장되지 않습니다.'
          : '커리큘럼을 배정했습니다.',
      )
    } catch (caught) {
      setAssignError(caught)
      if (shouldRefresh(caught)) {
        try {
          await onRefreshLearning()
        } catch {
          // Keep the command error. The learning reload can be retried from the page.
        }
      }
    } finally {
      setAssigning(false)
    }
  }

  async function onRelease() {
    if (!releaseTarget) {
      return
    }
    setReleasing(true)
    setReleaseError(null)
    try {
      await releaseStudentCurriculum(location.teacherStudentLocationId, releaseTarget.studentCurriculumId)
      await onRefreshLearning()
      setReleaseTarget(null)
      setNotice('커리큘럼 배정을 해제했습니다.')
    } catch (caught) {
      setReleaseError(caught)
      if (shouldRefresh(caught)) {
        try {
          await onRefreshLearning()
        } catch {
          // Keep the command error.
        }
      }
    } finally {
      setReleasing(false)
    }
  }

  async function onSaveMemo(item: StudentCurriculum) {
    setSavingMemoId(item.studentCurriculumId)
    setMemoError(null)
    setNotice('')
    try {
      const draft = memoDrafts[item.studentCurriculumId]
      await updateStudentCurriculumMemo(
        location.teacherStudentLocationId,
        item.studentCurriculumId,
        emptyToNull(draft ?? item.memo ?? ''),
      )
      await onRefreshLearning()
      setNotice('메모를 저장했습니다.')
    } catch (caught) {
      setMemoError(caught)
      if (shouldRefresh(caught)) {
        try {
          await onRefreshLearning()
        } catch {
          // Keep the command error.
        }
      }
    } finally {
      setSavingMemoId(null)
    }
  }

  return (
    <div className="curriculum-panel">
      {notice ? <p className="form-hint">{notice}</p> : null}
      {location.studentCurriculums.length === 0 ? <EmptyState message="배정된 커리큘럼이 없습니다." /> : null}
      {location.studentCurriculums.map((item) => (
        <div className="curriculum-assignment" key={item.studentCurriculumId}>
          <strong>{item.curriculumName}</strong>
          <label htmlFor={`memo-${item.studentCurriculumId}`}>메모</label>
          <input
            id={`memo-${item.studentCurriculumId}`}
            value={memoDrafts[item.studentCurriculumId] ?? item.memo ?? ''}
            onChange={(event) =>
              setMemoDrafts((current) => ({ ...current, [item.studentCurriculumId]: event.target.value }))
            }
          />
          <div className="row-actions">
            <button
              type="button"
              className="button button-quiet"
              disabled={savingMemoId === item.studentCurriculumId}
              onClick={() => void onSaveMemo(item)}
            >
              {savingMemoId === item.studentCurriculumId ? '저장 중' : '메모 저장'}
            </button>
            <button
              type="button"
              className="button button-quiet"
              onClick={() => {
                setReleaseError(null)
                setReleaseTarget(item)
              }}
            >
              배정 해제
            </button>
          </div>
        </div>
      ))}
      {memoError ? <p className="form-error">{formErrorMessage(memoError)}</p> : null}
      {catalogError ? <ErrorState error={catalogError} /> : null}
      {!catalogError && catalog == null ? <LoadingState label="커리큘럼 목록을 불러오는 중" /> : null}
      {catalog && available.length === 0 ? <p className="quiet">배정할 수 있는 커리큘럼이 없습니다.</p> : null}
      {catalog && available.length > 0 ? (
        <div className="assign-row">
          <label htmlFor={selectId}>배정할 커리큘럼 ({location.locationName})</label>
          <select id={selectId} value={selectedId} onChange={(event) => setSelectedId(event.target.value)} disabled={assigning}>
            <option value="">선택</option>
            {available.map((item) => (
              <option key={item.curriculumId} value={item.curriculumId}>
                {item.name}
              </option>
            ))}
          </select>
          <label htmlFor={`new-memo-${location.teacherStudentLocationId}`}>배정 메모</label>
          <input
            id={`new-memo-${location.teacherStudentLocationId}`}
            value={memo}
            onChange={(event) => setMemo(event.target.value)}
          />
          <button type="button" className="button" onClick={() => void onAssign()} disabled={assigning || selectedId === ''}>
            {assigning ? '배정 중' : '커리큘럼 배정'}
          </button>
        </div>
      ) : null}
      {assignError ? <p className="form-error">{assignmentErrorMessage(assignError)}</p> : null}
      {releaseTarget ? (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => !releasing && setReleaseTarget(null)}>
          <div
            className="modal"
            role="dialog"
            aria-labelledby={`release-curriculum-${releaseTarget.studentCurriculumId}`}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <h2 id={`release-curriculum-${releaseTarget.studentCurriculumId}`}>커리큘럼 배정 해제</h2>
            <p>
              이 학생과 {location.locationName}의 {releaseTarget.curriculumName} 배정을 해제합니다. 모니터링과 과제
              기록은 삭제되지 않습니다. 다시 배정하면 기존 관계가 다시 활성화될 수 있습니다.
            </p>
            {releaseError ? <p className="form-error">{formErrorMessage(releaseError)}</p> : null}
            <div className="modal-actions">
              <button type="button" className="button button-quiet" onClick={() => setReleaseTarget(null)} disabled={releasing}>
                취소
              </button>
              <button type="button" className="button button-danger" onClick={() => void onRelease()} disabled={releasing}>
                {releasing ? '해제 중' : '배정 해제'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function assignmentErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'COMMON_CONFLICT') {
    return '이미 이 출강처에 배정된 커리큘럼입니다.'
  }
  return formErrorMessage(error) ?? '요청을 처리하지 못했습니다.'
}

function shouldRefresh(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.code === 'COMMON_CONFLICT')
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
