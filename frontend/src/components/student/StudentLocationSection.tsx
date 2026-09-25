import { useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import { assignStudentLocation, releaseStudentLocation } from '../../api/locationApi.ts'
import { formErrorMessage } from '../feedback/describeError.ts'
import { EmptyState } from '../feedback/EmptyState.tsx'
import { ErrorState } from '../feedback/ErrorState.tsx'
import { LoadingState } from '../feedback/LoadingState.tsx'
import { textOrDash } from '../../student/display.ts'
import type { Curriculum } from '../../types/curriculum.ts'
import type { Location } from '../../types/location.ts'
import type { LearningLocation } from '../../types/student.ts'
import { StudentCurriculumPanel } from './StudentCurriculumPanel.tsx'

type Props = {
  studentId: number
  assigned: LearningLocation[]
  catalog: Location[] | null
  catalogError: unknown
  curriculums: Curriculum[] | null
  curriculumCatalogError: unknown
  onRefreshLearning: () => Promise<void>
}

export function StudentLocationSection({
  studentId,
  assigned,
  catalog,
  catalogError,
  curriculums,
  curriculumCatalogError,
  onRefreshLearning,
}: Props) {
  const [selectedId, setSelectedId] = useState('')
  const [notice, setNotice] = useState('')
  const [assignError, setAssignError] = useState<unknown>(null)
  const [assigning, setAssigning] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<LearningLocation | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releasing, setReleasing] = useState(false)

  const assignedIds = new Set(assigned.map((location) => location.locationId))
  const available = catalog?.filter((location) => !assignedIds.has(location.locationId)) ?? []

  async function onAssign() {
    const locationId = Number(selectedId)
    if (!Number.isInteger(locationId) || locationId <= 0) {
      return
    }
    setAssigning(true)
    setAssignError(null)
    setNotice('')
    try {
      await assignStudentLocation(studentId, locationId)
      await onRefreshLearning()
      setSelectedId('')
      setNotice('출강처를 배정했습니다.')
    } catch (caught) {
      setAssignError(caught)
      if (caught instanceof ApiError && caught.code === 'COMMON_CONFLICT') {
        try {
          await onRefreshLearning()
        } catch {
          // Keep the conflict message. The learning reload can be retried from the page.
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
      await releaseStudentLocation(studentId, releaseTarget.locationId)
      await onRefreshLearning()
      setReleaseTarget(null)
      setNotice('출강처 연결을 해제했습니다.')
    } catch (caught) {
      setReleaseError(caught)
    } finally {
      setReleasing(false)
    }
  }

  return (
    <section className="card">
      <h2>출강처</h2>
      {notice ? <p className="form-hint">{notice}</p> : null}
      {assigned.length === 0 ? <EmptyState message="배정된 출강처가 없습니다." /> : null}
      {assigned.map((location) => (
        <div className="location-block" key={location.teacherStudentLocationId}>
          <div className="assign-row">
            <div>
              <strong>{location.locationName}</strong>
              <p className="quiet">{textOrDash(location.address)}</p>
            </div>
            <button
              type="button"
              className="button button-quiet"
              onClick={() => {
                setReleaseError(null)
                setReleaseTarget(location)
              }}
            >
              해제
            </button>
          </div>
          <StudentCurriculumPanel
            location={location}
            catalog={curriculums}
            catalogError={curriculumCatalogError}
            onRefreshLearning={onRefreshLearning}
          />
        </div>
      ))}
      {catalogError ? <ErrorState error={catalogError} /> : null}
      {!catalogError && catalog == null ? <LoadingState label="출강처 목록을 불러오는 중" /> : null}
      {catalog && available.length === 0 ? <p className="quiet">배정할 수 있는 출강처가 없습니다.</p> : null}
      {catalog && available.length > 0 ? (
        <div className="assign-row">
          <label htmlFor="assign-location">배정할 출강처</label>
          <select
            id="assign-location"
            value={selectedId}
            onChange={(event) => setSelectedId(event.target.value)}
            disabled={assigning}
          >
            <option value="">선택</option>
            {available.map((location) => (
              <option key={location.locationId} value={location.locationId}>
                {location.name}
              </option>
            ))}
          </select>
          <button type="button" className="button" onClick={() => void onAssign()} disabled={assigning || selectedId === ''}>
            {assigning ? '배정 중' : '배정'}
          </button>
        </div>
      ) : null}
      {assignError ? <p className="form-error">{assignmentErrorMessage(assignError)}</p> : null}
      {releaseTarget ? (
        <ReleaseAssignmentDialog
          name={releaseTarget.locationName}
          submitting={releasing}
          error={releaseError}
          onClose={() => {
            if (!releasing) {
              setReleaseTarget(null)
            }
          }}
          onConfirm={() => void onRelease()}
        />
      ) : null}
    </section>
  )
}

function assignmentErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'COMMON_CONFLICT') {
    return '이미 배정된 출강처입니다.'
  }
  return formErrorMessage(error) ?? '요청을 처리하지 못했습니다.'
}

function ReleaseAssignmentDialog({
  name,
  submitting,
  error,
  onClose,
  onConfirm,
}: {
  name: string
  submitting: boolean
  error: unknown
  onClose: () => void
  onConfirm: () => void
}) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-labelledby="release-assignment-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h2 id="release-assignment-title">출강처 연결 해제</h2>
        <p>
          {name} 연결을 해제합니다. 이 연결의 활성 수강도 함께 비활성화됩니다. 모니터링과 과제 기록은 남고, 다시
          배정해도 기존 수강은 자동으로 돌아오지 않습니다.
        </p>
        {error ? <p className="form-error">{formErrorMessage(error)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className="button button-danger" onClick={onConfirm} disabled={submitting}>
            {submitting ? '해제 중' : '해제'}
          </button>
        </div>
      </div>
    </div>
  )
}
