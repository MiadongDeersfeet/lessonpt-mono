import { useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import {
  assignStudentCurriculum,
  releaseStudentCurriculum,
  updateStudentCurriculumMemo,
} from '../../api/studentCurriculumApi.ts'
import type { Curriculum } from '../../types/curriculum.ts'
import type { LearningLocation, StudentCurriculum } from '../../types/student.ts'
import { Toast } from '../feedback/Toast.tsx'
import { formErrorMessage } from '../feedback/describeError.ts'
import { ErrorState } from '../feedback/ErrorState.tsx'
import { LoadingState } from '../feedback/LoadingState.tsx'
import { MoreMenu } from '../layout/MoreMenu.tsx'
import { ProgressValue } from './ProgressValue.tsx'

type Props = {
  location: LearningLocation
  catalog: Curriculum[] | null
  catalogError: unknown
  onRefreshLearning: () => Promise<void>
  onOpenLearning: () => void
}

export function StudentCurriculumPanel({ location, catalog, catalogError, onRefreshLearning, onOpenLearning }: Props) {
  const [assignOpen, setAssignOpen] = useState(false)
  const [selectedId, setSelectedId] = useState('')
  const [memo, setMemo] = useState('')
  const [toast, setToast] = useState('')
  const [assignError, setAssignError] = useState<unknown>(null)
  const [assigning, setAssigning] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<StudentCurriculum | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releasing, setReleasing] = useState(false)
  const [memoDrafts, setMemoDrafts] = useState<Record<number, string>>({})
  const [editingMemoId, setEditingMemoId] = useState<number | null>(null)
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
    setToast('')
    try {
      const assigned = await assignStudentCurriculum(location.teacherStudentLocationId, {
        curriculumId,
        memo: emptyToNull(memo),
      })
      await onRefreshLearning()
      setSelectedId('')
      setMemo('')
      setAssignOpen(false)
      setToast(
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
      setToast('커리큘럼 배정을 해제했습니다.')
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

  function beginMemoEdit(item: StudentCurriculum) {
    setEditingMemoId(item.studentCurriculumId)
    setMemoDrafts((current) => ({ ...current, [item.studentCurriculumId]: item.memo ?? '' }))
    setMemoError(null)
  }

  async function onSaveMemo(item: StudentCurriculum, raw: string) {
    const next = emptyToNull(raw)
    const removing = next == null && hasText(item.memo)
    setSavingMemoId(item.studentCurriculumId)
    setMemoError(null)
    try {
      await updateStudentCurriculumMemo(location.teacherStudentLocationId, item.studentCurriculumId, next)
      await onRefreshLearning()
      setEditingMemoId(null)
      setToast(removing ? '메모를 삭제했습니다.' : '메모를 저장했습니다.')
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
      {location.studentCurriculums.length === 0 ? <p className="quiet">배정된 커리큘럼이 없습니다.</p> : (
        <div className="curriculum-columns" aria-hidden="true">
          <span>커리큘럼</span>
          <span>진행</span>
          <span>메모</span>
          <span>작업</span>
        </div>
      )}
      {location.studentCurriculums.map((item) => {
        const editing = editingMemoId === item.studentCurriculumId
        const memoText = hasText(item.memo) ? item.memo?.trim() ?? null : null
        const memoId = `memo-${item.studentCurriculumId}`
        return (
          <article className={editing ? 'curriculum-row is-editing' : 'curriculum-row'} key={item.studentCurriculumId}>
            <strong className="curriculum-name">{item.curriculumName}</strong>
            <div className="curriculum-progress">
              {item.progress ? <ProgressValue progress={item.progress} meter /> : null}
            </div>
            <div className="curriculum-memo">
              {editing ? (
                <div className="memo-editor">
                  <label className="sr-only" htmlFor={memoId}>메모</label>
                  <textarea
                    id={memoId}
                    value={memoDrafts[item.studentCurriculumId] ?? item.memo ?? ''}
                    onChange={(event) =>
                      setMemoDrafts((current) => ({ ...current, [item.studentCurriculumId]: event.target.value }))
                    }
                  />
                  <div className="row-actions">
                    <button type="button" className="text-action" onClick={() => setEditingMemoId(null)} disabled={savingMemoId === item.studentCurriculumId}>
                      취소
                    </button>
                    <button
                      type="button"
                      className="button"
                      disabled={savingMemoId === item.studentCurriculumId}
                      onClick={() => void onSaveMemo(item, memoDrafts[item.studentCurriculumId] ?? item.memo ?? '')}
                    >
                      {savingMemoId === item.studentCurriculumId ? '저장 중' : '저장'}
                    </button>
                    {memoText ? (
                      <button type="button" className="text-button" disabled={savingMemoId === item.studentCurriculumId} onClick={() => void onSaveMemo(item, '')}>
                        메모 삭제
                      </button>
                    ) : null}
                  </div>
                </div>
              ) : (
                <p className="memo-line">
                  <span title={memoText ?? undefined}>{memoText ? previewMemo(memoText) : '메모 없음'}</span>
                  <button type="button" className="text-action" aria-label={memoText ? '메모 편집' : '메모 추가'} onClick={() => beginMemoEdit(item)}>
                    {memoText ? '편집' : '추가'}
                  </button>
                </p>
              )}
            </div>
            <div className="curriculum-actions">
              <button type="button" className="text-action" onClick={onOpenLearning}>학습관리</button>
              <MoreMenu
                label={`${item.curriculumName} 작업`}
                items={[{
                  label: '커리큘럼 배정 해제',
                  danger: true,
                  onSelect: () => {
                    setReleaseError(null)
                    setReleaseTarget(item)
                  },
                }]}
              />
            </div>
          </article>
        )
      })}
      {toast ? <Toast message={toast} onDone={() => setToast('')} /> : null}
      {memoError ? <p className="form-error">{formErrorMessage(memoError)}</p> : null}
      {catalogError ? <ErrorState error={catalogError} /> : null}
      {!catalogError && catalog == null ? <LoadingState label="커리큘럼 목록을 불러오는 중" /> : null}
      {catalog && available.length === 0 ? <p className="quiet">이 출강처에 추가로 배정할 수 있는 커리큘럼이 없습니다.</p> : null}
      {catalog && available.length > 0 && !assignOpen ? (
        <button type="button" className="text-action assign-action group-action" onClick={() => { setAssignError(null); setAssignOpen(true) }}>
          + 커리큘럼 배정
        </button>
      ) : null}
      {catalog && available.length > 0 && assignOpen ? (
        <div className="assign-form group-action">
          <label htmlFor={selectId}>배정할 커리큘럼 ({location.locationName})</label>
          <select id={selectId} value={selectedId} onChange={(event) => setSelectedId(event.target.value)} disabled={assigning}>
            <option value="">선택</option>
            {available.map((item) => (
              <option key={item.curriculumId} value={item.curriculumId}>{item.name}</option>
            ))}
          </select>
          <label htmlFor={`new-memo-${location.teacherStudentLocationId}`}>배정 메모</label>
          <input id={`new-memo-${location.teacherStudentLocationId}`} value={memo} onChange={(event) => setMemo(event.target.value)} />
          <div className="row-actions">
            <button
              type="button"
              className="text-action"
              disabled={assigning}
              onClick={() => {
                setAssignOpen(false)
                setSelectedId('')
                setMemo('')
                setAssignError(null)
              }}
            >
              취소
            </button>
            <button type="button" className="button" onClick={() => void onAssign()} disabled={assigning || selectedId === ''}>
              {assigning ? '배정 중' : '커리큘럼 배정'}
            </button>
          </div>
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
                {releasing ? '해제 중' : '커리큘럼 배정 해제'}
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

function hasText(value: string | null): boolean {
  return value != null && value.trim() !== ''
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function previewMemo(value: string): string {
  if (value.length <= 42) {
    return value
  }
  return `${value.slice(0, 42)}…`
}
