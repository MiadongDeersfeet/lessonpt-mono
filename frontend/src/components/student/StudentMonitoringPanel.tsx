import { useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import { createMonitoring, deactivateMonitoring, updateMonitoring } from '../../api/studentMonitoringApi.ts'
import type { ContentDetail } from '../../types/curriculum.ts'
import type { Monitoring, ProgressStatus } from '../../types/student.ts'
import { progressStatusLabel } from '../../student/display.ts'
import { formErrorMessage } from '../feedback/describeError.ts'
import { MoreMenu } from '../layout/MoreMenu.tsx'
import { Overlay } from '../layout/Overlay.tsx'
import { ResizableTable } from '../layout/ResizableTable.tsx'
import { ResourceButtons } from '../curriculum/ResourceButtons.tsx'
import { BpmGauge } from './BpmGauge.tsx'
import { HomeworkPanel } from './HomeworkPanel.tsx'

const statuses: ProgressStatus[] = ['YET', 'IN_PROGRESS', 'COMPLETED', 'STOPPED']

type ContentOption = ContentDetail & { categoryId: number; categoryName: string }
const columns = [
  { key: 'name', label: '내용', width: 280, min: 180 },
  { key: 'status', label: '상태', width: 120, min: 96 },
  { key: 'bpm', label: '현재 / 목표 BPM', width: 180, min: 150 },
  { key: 'resources', label: '자료', width: 180, min: 120 },
  { key: 'actions', label: '작업', width: 188, min: 150 },
]
const statusMark: Record<ProgressStatus, string> = {
  YET: '○',
  IN_PROGRESS: '●',
  COMPLETED: '●',
  STOPPED: '–',
}

type Props = {
  curriculumId: number
  studentCurriculumId: number
  monitorings: Monitoring[]
  contents: ContentOption[] | null
  catalogError: unknown
  onRefreshLearning: () => Promise<void>
}

export function StudentMonitoringPanel({
  curriculumId,
  studentCurriculumId,
  monitorings,
  contents,
  catalogError,
  onRefreshLearning,
}: Props) {
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [homeworkBusy, setHomeworkBusy] = useState(false)
  const [homeworkId, setHomeworkId] = useState<number | null>(null)
  const [connectingId, setConnectingId] = useState<number | null>(null)
  const [createError, setCreateError] = useState<unknown>(null)
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

  const categories = groupByCategory(contents ?? [])
  const selected = categories.find((category) => category.categoryId === categoryId) ?? categories[0] ?? null
  const monitoringByContentId = new Map(monitorings.map((item) => [item.contentDetailId, item]))

  async function onConnect(contentDetailId: number) {
    setConnectingId(contentDetailId)
    setCreateError(null)
    setNotice('')
    try {
      await createMonitoring(studentCurriculumId, {
        contentDetailId,
        currentBpm: null,
        progressStatus: 'YET',
        memo: null,
      })
      await onRefreshLearning()
      setNotice('학습 내용을 연결했습니다.')
    } catch (caught) {
      setCreateError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setConnectingId(null)
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
      setNotice('학습 연결을 해제했습니다.')
    } catch (caught) {
      setReleaseError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setReleasing(false)
    }
  }

  const categorySelectId = `lesson-category-${studentCurriculumId}`

  return (
    <div className="monitoring-panel">
      {notice ? <p className="form-hint">{notice}</p> : null}
      {catalogError ? <p className="form-error">{formErrorMessage(catalogError)}</p> : null}
      {contents == null && !catalogError ? <p className="quiet">내용 목록을 불러오는 중</p> : null}
      {contents && categories.length === 0 ? <p className="quiet">등록된 내용이 없습니다.</p> : null}
      {selected ? (
        <div className="lesson-toolbar">
          <div className="lesson-category">
            <label htmlFor={categorySelectId}>카테고리</label>
            <select
              id={categorySelectId}
              className="lesson-category-select"
              value={selected.categoryId}
              onChange={(event) => setCategoryId(Number(event.target.value))}
            >
              {categories.map((category) => (
                <option key={category.categoryId} value={category.categoryId}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
          <div className="lesson-sheet">
          <ResizableTable columns={columns} storageKey="teacher-learning" label={`${selected.name} 학습 내용`} fill>
            {selected.contents.map((content) => {
              const linked = monitoringByContentId.get(content.contentDetailId)
              const resources = <ResourceButtons content={content} blankWhenEmpty teacher={{ curriculumId, categoryId: content.categoryId, contentDetailId: content.contentDetailId }} />
              if (!linked) {
                return (
                  <tr key={content.contentDetailId} className="content-unlinked">
                    <td data-label="내용" className="content-name">{content.name}</td>
                    <td data-label="상태"><span className="lesson-status lesson-status-unlinked"><span aria-hidden="true">○</span>미연결</span></td>
                    <td data-label="현재 / 목표 BPM" />
                    <td data-label="자료">{resources}</td>
                    <td data-label="작업">
                      <button
                        type="button"
                        className="text-action connect-action"
                        disabled={connectingId === content.contentDetailId}
                        onClick={() => void onConnect(content.contentDetailId)}
                      >
                        {connectingId === content.contentDetailId ? '연결 중' : '연결'}
                      </button>
                    </td>
                  </tr>
                )
              }
              return (
                <tr key={linked.monitoringId}>
                  <td data-label="내용" className="content-name">{linked.contentDetailName}</td>
                  <td data-label="상태">
                    <span className={`lesson-status lesson-status-${linked.progressStatus}`}>
                      <span aria-hidden="true">{statusMark[linked.progressStatus]}</span>
                      {progressStatusLabel(linked.progressStatus)}
                    </span>
                  </td>
                  <td data-label="현재 / 목표 BPM"><BpmGauge current={linked.currentBpm} target={linked.targetBpm} /></td>
                  <td data-label="자료">{resources}</td>
                  <td data-label="작업">
                    <div className="row-actions">
                      <button type="button" className="text-action" aria-label="기록 수정" onClick={() => startEdit(linked)}>수정</button>
                      <button type="button" className="text-action" aria-label={`과제 ${linked.homeworks.length}개`} onClick={() => setHomeworkId(linked.monitoringId)}>과제 {linked.homeworks.length}</button>
                      <MoreMenu
                        label={`${linked.contentDetailName} 학습 작업`}
                        items={[{
                          label: '연결 해제',
                          danger: true,
                          onSelect: () => {
                            setReleaseError(null)
                            setReleaseTarget(linked)
                          },
                        }]}
                      />
                    </div>
                  </td>
                </tr>
              )
            })}
          </ResizableTable>
          </div>
        </div>
      ) : null}
      {monitorings.filter((item) => item.monitoringId === editingId).map((item) => (
        <Overlay key={item.monitoringId} title={`${item.contentDetailName} 기록 수정`} className="content-editor" onClose={() => { if (!saving) setEditingId(null) }}>
          <div className="overlay-body">
            <div className="stack">
              <label htmlFor={`status-${item.monitoringId}`}>학습 상태</label>
              <select id={`status-${item.monitoringId}`} value={editStatus} onChange={(event) => setEditStatus(event.target.value as ProgressStatus)}>
                {statuses.map((status) => (
                  <option key={status} value={status}>{progressStatusLabel(status)}</option>
                ))}
              </select>
              <label htmlFor={`bpm-${item.monitoringId}`}>현재 BPM</label>
              <input id={`bpm-${item.monitoringId}`} inputMode="numeric" value={editBpm} onChange={(event) => setEditBpm(event.target.value)} />
              {editBpmError ? <p className="field-error">{editBpmError}</p> : null}
              {fieldErrorMessageSafe(editError, 'currentBpm') ? <p className="field-error">{fieldErrorMessageSafe(editError, 'currentBpm')}</p> : null}
              <label htmlFor={`memo-${item.monitoringId}`}>메모</label>
              <input id={`memo-${item.monitoringId}`} value={editMemo} onChange={(event) => setEditMemo(event.target.value)} />
              {editError ? <p className="form-error">{formErrorMessage(editError)}</p> : null}
              <div className="row-actions">
                <button type="button" className="button" disabled={saving} onClick={() => void onSave(item)}>{saving ? '저장 중' : '저장'}</button>
                <button type="button" className="button button-quiet" onClick={() => setEditingId(null)} disabled={saving}>취소</button>
              </div>
            </div>
          </div>
        </Overlay>
      ))}
      {monitorings.filter((item) => item.monitoringId === homeworkId).map((item) => (
        <Overlay key={item.monitoringId} title={`${item.contentDetailName} 과제`} className="content-editor" onClose={() => { if (!homeworkBusy) setHomeworkId(null) }}>
          <div className="overlay-body">
            <HomeworkPanel onBusyChange={setHomeworkBusy} monitoringId={item.monitoringId} contentName={item.contentDetailName} homeworks={item.homeworks} onRefreshLearning={onRefreshLearning} />
          </div>
        </Overlay>
      ))}
      {createError ? <p className="form-error">{createErrorMessage(createError)}</p> : null}
      {releaseTarget ? (
        <div className="modal-backdrop" role="presentation" onMouseDown={() => !releasing && setReleaseTarget(null)}>
          <div className="modal" role="dialog" aria-labelledby="deactivate-monitoring" onMouseDown={(event) => event.stopPropagation()}>
            <h2 id="deactivate-monitoring">연결 해제</h2>
            <p>
              {releaseTarget.contentDetailName} 학습 연결을 해제합니다. 이 기록의 활성 과제도 함께 해제됩니다.
              커리큘럼 배정과 내용 자체는 지우지 않습니다. 다시 연결해도 과제는 자동으로 돌아오지 않습니다.
            </p>
            {releaseError ? <p className="form-error">{formErrorMessage(releaseError)}</p> : null}
            <div className="modal-actions">
              <button type="button" className="button button-quiet" disabled={releasing} onClick={() => setReleaseTarget(null)}>취소</button>
              <button type="button" className="button button-danger" disabled={releasing} onClick={() => void onDeactivate()}>
                {releasing ? '처리 중' : '연결 해제'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function groupByCategory(contents: ContentOption[]) {
  const groups: { categoryId: number; name: string; contents: ContentOption[] }[] = []
  for (const content of contents) {
    const current = groups.find((group) => group.categoryId === content.categoryId)
    if (current) {
      current.contents.push(content)
    } else {
      groups.push({ categoryId: content.categoryId, name: content.categoryName, contents: [content] })
    }
  }
  return groups
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
