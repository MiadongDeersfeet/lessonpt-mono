import { useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import { assignStudentLocation, releaseStudentLocation } from '../../api/locationApi.ts'
import type { Curriculum } from '../../types/curriculum.ts'
import type { Location } from '../../types/location.ts'
import type { LearningLocation } from '../../types/student.ts'
import { Toast } from '../feedback/Toast.tsx'
import { formErrorMessage } from '../feedback/describeError.ts'
import { EmptyState } from '../feedback/EmptyState.tsx'
import { ErrorState } from '../feedback/ErrorState.tsx'
import { LoadingState } from '../feedback/LoadingState.tsx'
import { MoreMenu } from '../layout/MoreMenu.tsx'
import { StudentCurriculumPanel } from './StudentCurriculumPanel.tsx'

type Props = {
  studentId: number
  assigned: LearningLocation[]
  catalog: Location[] | null
  catalogError: unknown
  curriculums: Curriculum[] | null
  curriculumCatalogError: unknown
  onRefreshLearning: () => Promise<void>
  onOpenLearning: () => void
}

export function StudentLocationSection({
  studentId,
  assigned,
  catalog,
  catalogError,
  curriculums,
  curriculumCatalogError,
  onRefreshLearning,
  onOpenLearning,
}: Props) {
  const [assignOpen, setAssignOpen] = useState(false)
  const [selectedId, setSelectedId] = useState('')
  const [toast, setToast] = useState('')
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
    setToast('')
    try {
      await assignStudentLocation(studentId, locationId)
      await onRefreshLearning()
      setSelectedId('')
      setAssignOpen(false)
      setToast('출강처를 배정했습니다.')
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
      setToast('출강처 배정을 해제했습니다.')
    } catch (caught) {
      setReleaseError(caught)
    } finally {
      setReleasing(false)
    }
  }

  return (
    <section className="relationship-section">
      <h2>출강처 및 커리큘럼</h2>
      {assigned.length === 0 ? <EmptyState message="배정된 출강처가 없습니다." /> : null}
      {assigned.map((location) => (
        <section className="location-group" aria-label={location.locationName} key={location.teacherStudentLocationId}>
          <header className="location-group-header">
            <h3>
              {location.locationName}
              {location.address ? <span className="location-meta"> · {location.address}</span> : null}
            </h3>
            <MoreMenu
              label={`${location.locationName} 작업`}
              items={[{
                label: '출강처 배정 해제',
                danger: true,
                onSelect: () => {
                  setReleaseError(null)
                  setReleaseTarget(location)
                },
              }]}
            />
          </header>
          <StudentCurriculumPanel
            location={location}
            catalog={curriculums}
            catalogError={curriculumCatalogError}
            onRefreshLearning={onRefreshLearning}
            onOpenLearning={onOpenLearning}
          />
        </section>
      ))}
      {catalogError ? <ErrorState error={catalogError} /> : null}
      {!catalogError && catalog == null ? <LoadingState label="출강처 목록을 불러오는 중" /> : null}
      {catalog && available.length === 0 ? <p className="quiet">배정할 수 있는 출강처가 없습니다.</p> : null}
      {catalog && available.length > 0 && !assignOpen ? (
        <button type="button" className="text-action assign-action section-action" onClick={() => { setAssignError(null); setAssignOpen(true) }}>
          + 출강처 배정
        </button>
      ) : null}
      {catalog && available.length > 0 && assignOpen ? (
        <div className="assign-form section-action">
          <label htmlFor="assign-location">배정할 출강처</label>
          <select id="assign-location" value={selectedId} onChange={(event) => setSelectedId(event.target.value)} disabled={assigning}>
            <option value="">선택</option>
            {available.map((location) => (
              <option key={location.locationId} value={location.locationId}>{location.name}</option>
            ))}
          </select>
          <div className="row-actions">
            <button
              type="button"
              className="text-action"
              disabled={assigning}
              onClick={() => {
                setAssignOpen(false)
                setSelectedId('')
                setAssignError(null)
              }}
            >
              취소
            </button>
            <button type="button" className="button" onClick={() => void onAssign()} disabled={assigning || selectedId === ''}>
              {assigning ? '배정 중' : '배정'}
            </button>
          </div>
        </div>
      ) : null}
      {assignError ? <p className="form-error">{assignmentErrorMessage(assignError)}</p> : null}
      {toast ? <Toast message={toast} onDone={() => setToast('')} /> : null}
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
      <div className="modal" role="dialog" aria-labelledby="release-assignment-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="release-assignment-title">출강처 배정 해제</h2>
        <p>
          {name} 배정을 해제합니다. 이 출강처의 활성 수강도 함께 해제됩니다. 모니터링과 과제 기록은 남고, 다시
          배정해도 기존 수강은 자동으로 돌아오지 않습니다.
        </p>
        {error ? <p className="form-error">{formErrorMessage(error)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className="button button-danger" onClick={onConfirm} disabled={submitting}>
            {submitting ? '해제 중' : '출강처 배정 해제'}
          </button>
        </div>
      </div>
    </div>
  )
}
