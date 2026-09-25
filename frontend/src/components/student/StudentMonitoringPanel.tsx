import { useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import { createMonitoring, deactivateMonitoring, updateMonitoring } from '../../api/studentMonitoringApi.ts'
import { formErrorMessage } from '../feedback/describeError.ts'
import { progressStatusLabel } from '../../student/display.ts'
import type { Monitoring, ProgressStatus } from '../../types/student.ts'

const statuses: ProgressStatus[] = ['YET', 'IN_PROGRESS', 'COMPLETED', 'STOPPED']

type ContentOption = {
  contentDetailId: number
  name: string
}

type Props = {
  studentCurriculumId: number
  monitorings: Monitoring[]
  contents: ContentOption[] | null
  catalogError: unknown
  onRefreshLearning: () => Promise<void>
}

export function StudentMonitoringPanel({
  studentCurriculumId,
  monitorings,
  contents,
  catalogError,
  onRefreshLearning,
}: Props) {
  const [selectedId, setSelectedId] = useState('')
  const [createStatus, setCreateStatus] = useState<ProgressStatus>('YET')
  const [createBpm, setCreateBpm] = useState('')
  const [createMemo, setCreateMemo] = useState('')
  const [createError, setCreateError] = useState<unknown>(null)
  const [bpmError, setBpmError] = useState('')
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editStatus, setEditStatus] = useState<ProgressStatus>('YET')
  const [editBpm, setEditBpm] = useState('')
  const [editMemo, setEditMemo] = useState('')
  const [editError, setEditError] = useState<unknown>(null)
  const [editBpmError, setEditBpmError] = useState('')
  const [saving, setSaving] = useState(false)
  const [releaseTarget, setReleaseTarget] = useState<Monitoring | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releasing, setReleasing] = useState(false)

  const monitoredIds = new Set(monitorings.map((item) => item.contentDetailId))
  const available = contents?.filter((item) => !monitoredIds.has(item.contentDetailId)) ?? []

  async function onCreate() {
    const contentDetailId = Number(selectedId)
    if (!Number.isInteger(contentDetailId) || contentDetailId <= 0) {
      return
    }
    const bpm = parseBpm(createBpm)
    if (bpm === 'invalid') {
      setBpmError('현재 BPM은 60 이상 240 이하여야 합니다.')
      return
    }
    setBpmError('')
    setCreating(true)
    setCreateError(null)
    setNotice('')
    try {
      await createMonitoring(studentCurriculumId, {
        contentDetailId,
        currentBpm: bpm,
        progressStatus: createStatus,
        memo: emptyToNull(createMemo),
      })
      await onRefreshLearning()
      setSelectedId('')
      setCreateBpm('')
      setCreateMemo('')
      setCreateStatus('YET')
      setNotice('학습 기록을 추가했습니다.')
    } catch (caught) {
      setCreateError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setCreating(false)
    }
  }

  function startEdit(item: Monitoring) {
    setEditingId(item.monitoringId)
    setEditStatus(item.progressStatus)
    setEditBpm(item.currentBpm == null ? '' : String(item.currentBpm))
    setEditMemo(item.memo ?? '')
    setEditError(null)
    setEditBpmError('')
  }

  async function onSave(item: Monitoring) {
    const bpm = parseBpm(editBpm)
    if (bpm === 'invalid') {
      setEditBpmError('현재 BPM은 60 이상 240 이하여야 합니다.')
      return
    }
    setEditBpmError('')
    setSaving(true)
    setEditError(null)
    setNotice('')
    try {
      await updateMonitoring(studentCurriculumId, item.monitoringId, {
        currentBpm: bpm,
        progressStatus: editStatus,
        memo: emptyToNull(editMemo),
      })
      await onRefreshLearning()
      setEditingId(null)
      setNotice('학습 기록을 수정했습니다.')
    } catch (caught) {
      setEditError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setSaving(false)
    }
  }

  async function onDeactivate() {
    if (!releaseTarget) {
      return
    }
    setReleasing(true)
    setReleaseError(null)
    try {
      await deactivateMonitoring(studentCurriculumId, releaseTarget.monitoringId)
      await onRefreshLearning()
      setReleaseTarget(null)
      setNotice('학습 기록을 비활성화했습니다.')
    } catch (caught) {
      setReleaseError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setReleasing(false)
    }
  }

  return (
    <div className="monitoring-panel">
      {notice ? <p className="form-hint">{notice}</p> : null}
      {monitorings.length === 0 ? <p className="quiet">아직 등록된 학습 기록이 없습니다.</p> : null}
      {monitorings.map((item) => (
        <div className="monitoring" key={item.monitoringId}>
          <h4>{item.contentDetailName}</h4>
          {editingId === item.monitoringId ? (
            <div className="stack">
              <label htmlFor={`status-${item.monitoringId}`}>학습 상태</label>
              <select
                id={`status-${item.monitoringId}`}
                value={editStatus}
                onChange={(event) => setEditStatus(event.target.value as ProgressStatus)}
              >
                {statuses.map((status) => (
                  <option key={status} value={status}>
                    {progressStatusLabel(status)}
                  </option>
                ))}
              </select>
              <label htmlFor={`bpm-${item.monitoringId}`}>현재 BPM</label>
              <input
                id={`bpm-${item.monitoringId}`}
                inputMode="numeric"
                value={editBpm}
                onChange={(event) => setEditBpm(event.target.value)}
              />
              {editBpmError ? <p className="field-error">{editBpmError}</p> : null}
              {fieldErrorMessageSafe(editError, 'currentBpm') ? (
                <p className="field-error">{fieldErrorMessageSafe(editError, 'currentBpm')}</p>
              ) : null}
              <label htmlFor={`memo-${item.monitoringId}`}>메모</label>
              <input
                id={`memo-${item.monitoringId}`}
                value={editMemo}
                onChange={(event) => setEditMemo(event.target.value)}
              />
              {editError ? <p className="form-error">{formErrorMessage(editError)}</p> : null}
              <div className="row-actions">
                <button type="button" className="button" disabled={saving} onClick={() => void onSave(item)}>
                  {saving ? '저장 중' : '저장'}
                </button>
                <button type="button" className="button button-quiet" onClick={() => setEditingId(null)} disabled={saving}>
                  취소
                </button>
              </div>
            </div>
          ) : (
            <div className="row-actions">
              <span>{progressStatusLabel(item.progressStatus)}</span>
              <span>현재 BPM {item.currentBpm == null ? '-' : item.currentBpm}</span>
              <button type="button" className="button button-quiet" onClick={() => startEdit(item)}>
                기록 수정
              </button>
              <button
                type="button"
                className="button button-quiet"
                onClick={() => {
                  setReleaseError(null)
                  setReleaseTarget(item)
                }}
              >
                비활성화
              </button>
            </div>
          )}
        </div>
      ))}
      {catalogError ? <p className="form-error">{formErrorMessage(catalogError)}</p> : null}
      {contents == null && !catalogError ? <p className="quiet">내용 목록을 불러오는 중</p> : null}
      {contents && available.length === 0 ? <p className="quiet">기록을 추가할 내용이 없습니다.</p> : null}
      {contents && available.length > 0 ? (
        <div className="assign-row">
          <label htmlFor={`new-content-${studentCurriculumId}`}>학습 기록 내용</label>
          <select id={`new-content-${studentCurriculumId}`} value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>
            <option value="">선택</option>
            {available.map((item) => (
              <option key={item.contentDetailId} value={item.contentDetailId}>
                {item.name}
              </option>
            ))}
          </select>
          <label htmlFor={`new-status-${studentCurriculumId}`}>초기 상태</label>
          <select
            id={`new-status-${studentCurriculumId}`}
            value={createStatus}
            onChange={(event) => setCreateStatus(event.target.value as ProgressStatus)}
          >
            {statuses.map((status) => (
              <option key={status} value={status}>
                {progressStatusLabel(status)}
              </option>
            ))}
          </select>
          <label htmlFor={`new-bpm-${studentCurriculumId}`}>현재 BPM</label>
          <input
            id={`new-bpm-${studentCurriculumId}`}
            inputMode="numeric"
            value={createBpm}
            onChange={(event) => setCreateBpm(event.target.value)}
          />
          {bpmError ? <p className="field-error">{bpmError}</p> : null}
          <button type="button" className="button" disabled={creating || selectedId === ''} onClick={() => void onCreate()}>
            {creating ? '추가 중' : '학습 기록 추가'}
          </button>
        </div>
      ) : null}
      {createError ? <p className="form-error">{createErrorMessage(createError)}</p> : null}
      {releaseTarget ? (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => !releasing && setReleaseTarget(null)}>
          <div className="modal" role="dialog" aria-labelledby="deactivate-monitoring" onMouseDown={(event) => event.stopPropagation()}>
            <h2 id="deactivate-monitoring">학습 기록 비활성화</h2>
            <p>
              {releaseTarget.contentDetailName} 학습 기록을 비활성화합니다. 이 기록의 활성 과제도 함께 비활성화됩니다.
              커리큘럼 배정과 내용 자체는 지우지 않습니다. 학습 기록을 다시 활성화해도 과제는 자동으로 돌아오지 않습니다.
            </p>
            {releaseError ? <p className="form-error">{formErrorMessage(releaseError)}</p> : null}
            <div className="modal-actions">
              <button type="button" className="button button-quiet" disabled={releasing} onClick={() => setReleaseTarget(null)}>
                취소
              </button>
              <button type="button" className="button button-danger" disabled={releasing} onClick={() => void onDeactivate()}>
                {releasing ? '처리 중' : '비활성화'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function parseBpm(value: string): number | null | 'invalid' {
  const trimmed = value.trim()
  if (trimmed === '') {
    return null
  }
  if (!/^\d+$/.test(trimmed)) {
    return 'invalid'
  }
  const parsed = Number(trimmed)
  if (parsed < 60 || parsed > 240) {
    return 'invalid'
  }
  return parsed
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function createErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'COMMON_CONFLICT') {
    return '이미 이 내용의 학습 기록이 있습니다.'
  }
  return formErrorMessage(error) ?? '요청을 처리하지 못했습니다.'
}

function fieldErrorMessageSafe(error: unknown, field: string): string | null {
  if (!(error instanceof ApiError)) {
    return null
  }
  return error.fieldErrors.find((item) => item.field === field)?.message ?? null
}

function shouldRefresh(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.code === 'COMMON_CONFLICT')
}

async function refreshQuietly(refresh: () => Promise<void>) {
  try {
    await refresh()
  } catch {
    // Keep the command error.
  }
}
