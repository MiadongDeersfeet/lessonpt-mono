import { useEffect, useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import { createHomework, deactivateHomework, updateHomework } from '../../api/homeworkApi.ts'
import { formErrorMessage } from '../feedback/describeError.ts'
import { formatDeadline, textOrDash } from '../../student/display.ts'
import type { Homework } from '../../types/student.ts'

import { Overlay } from '../layout/Overlay.tsx'

const maxActiveHomeworks = 3

type Props = {
  onBusyChange?: (busy: boolean) => void
  monitoringId: number
  contentName: string
  homeworks: Homework[]
  onRefreshLearning: () => Promise<void>
}

export function HomeworkPanel({ monitoringId, contentName, homeworks, onRefreshLearning, onBusyChange }: Props) {
  const [content, setContent] = useState('')
  const [deadline, setDeadline] = useState('')
  const [feedback, setFeedback] = useState('')
  const [contentError, setContentError] = useState('')
  const [createError, setCreateError] = useState<unknown>(null)
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editContent, setEditContent] = useState('')
  const [editDeadline, setEditDeadline] = useState('')
  const [editFeedback, setEditFeedback] = useState('')
  const [editContentError, setEditContentError] = useState('')
  const [editError, setEditError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [releaseTarget, setReleaseTarget] = useState<Homework | null>(null)
  const [releaseError, setReleaseError] = useState<unknown>(null)
  const [releasing, setReleasing] = useState(false)

  useEffect(() => { onBusyChange?.(creating || saving || busyId != null || releasing) }, [creating, saving, busyId, releasing, onBusyChange])

  const atLimit = homeworks.length >= maxActiveHomeworks
  const contentLabel = `과제 내용 (${contentName})`
  const deadlineLabel = `마감 (${contentName})`
  const feedbackLabel = `피드백 (${contentName})`

  async function onCreate() {
    if (content.trim() === '') {
      setContentError('과제 내용은 필수입니다.')
      return
    }
    setContentError('')
    setCreating(true)
    setCreateError(null)
    setNotice('')
    try {
      await createHomework(monitoringId, {
        homeworkContent: content.trim(),
        deadline: fromDateInput(deadline),
        feedback: emptyToNull(feedback),
      })
      await onRefreshLearning()
      setContent('')
      setDeadline('')
      setFeedback('')
      setNotice('과제를 추가했습니다.')
    } catch (caught) {
      setCreateError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setCreating(false)
    }
  }

  function startEdit(item: Homework) {
    setEditingId(item.homeworkId)
    setEditContent(item.homeworkContent)
    setEditDeadline(toDateInput(item.deadline))
    setEditFeedback(item.feedback ?? '')
    setEditContentError('')
    setEditError(null)
  }

  async function onSave(item: Homework) {
    if (editContent.trim() === '') {
      setEditContentError('과제 내용은 필수입니다.')
      return
    }
    setEditContentError('')
    setSaving(true)
    setEditError(null)
    setNotice('')
    try {
      await updateHomework(monitoringId, item.homeworkId, {
        homeworkContent: editContent.trim(),
        deadline: fromDateInput(editDeadline),
        feedback: emptyToNull(editFeedback),
      })
      await onRefreshLearning()
      setEditingId(null)
      setNotice('과제를 수정했습니다.')
    } catch (caught) {
      setEditError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setSaving(false)
    }
  }

  async function onCompleted(item: Homework, completed: boolean) {
    setBusyId(item.homeworkId)
    setNotice('')
    setCreateError(null)
    try {
      await updateHomework(monitoringId, item.homeworkId, { completed })
      await onRefreshLearning()
      setNotice(completed ? '과제를 완료했습니다.' : '과제 완료를 취소했습니다.')
    } catch (caught) {
      setCreateError(caught)
      if (shouldRefresh(caught)) {
        await refreshQuietly(onRefreshLearning)
      }
    } finally {
      setBusyId(null)
    }
  }

  async function onDeactivate() {
    if (!releaseTarget) {
      return
    }
    setReleasing(true)
    setReleaseError(null)
    try {
      await deactivateHomework(monitoringId, releaseTarget.homeworkId)
      await onRefreshLearning()
      setReleaseTarget(null)
      setNotice('과제를 삭제했습니다.')
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
    <div className="homework">
      <h5>과제</h5>
      {notice ? <p className="form-hint">{notice}</p> : null}
      {homeworks.length === 0 ? <p className="quiet">등록된 과제가 없습니다.</p> : null}
      {homeworks.map((item) => (
        <div className="homework-item" key={item.homeworkId}>
          {editingId === item.homeworkId ? (
            <div className="stack">
              <label htmlFor={`edit-hw-content-${item.homeworkId}`}>수정할 {contentLabel}</label>
              <input
                id={`edit-hw-content-${item.homeworkId}`}
                value={editContent}
                onChange={(event) => setEditContent(event.target.value)}
              />
              {editContentError ? <p className="field-error">{editContentError}</p> : null}
              {fieldMessage(editError, 'homeworkContent') ? (
                <p className="field-error">{fieldMessage(editError, 'homeworkContent')}</p>
              ) : null}
              <label htmlFor={`edit-hw-deadline-${item.homeworkId}`}>수정할 {deadlineLabel}</label>
              <input
                id={`edit-hw-deadline-${item.homeworkId}`}
                type="date"
                value={editDeadline}
                onChange={(event) => setEditDeadline(event.target.value)}
              />
              <label htmlFor={`edit-hw-feedback-${item.homeworkId}`}>수정할 {feedbackLabel}</label>
              <input
                id={`edit-hw-feedback-${item.homeworkId}`}
                value={editFeedback}
                onChange={(event) => setEditFeedback(event.target.value)}
              />
              {fieldMessage(editError, 'feedback') ? (
                <p className="field-error">{fieldMessage(editError, 'feedback')}</p>
              ) : null}
              {editError ? <p className="form-error">{formErrorMessage(editError)}</p> : null}
              <div className="row-actions">
                <button type="button" className="button" disabled={saving} onClick={() => void onSave(item)}>
                  {saving ? '저장 중' : '과제 저장'}
                </button>
                <button type="button" className="button button-quiet" disabled={saving} onClick={() => setEditingId(null)}>
                  취소
                </button>
              </div>
            </div>
          ) : (
            <>
              <p>{item.homeworkContent}</p>
              <div className="row-actions">
                <span>{item.completed === true ? '완료' : '미완료'}</span>
                <span>마감 {formatDeadline(item.deadline)}</span>
                <span>피드백 {textOrDash(item.feedback)}</span>
                <button type="button" className="button button-quiet" onClick={() => startEdit(item)}>
                  과제 수정
                </button>
                {item.completed === true ? (
                  <button
                    type="button"
                    className="button button-quiet"
                    disabled={busyId === item.homeworkId}
                    onClick={() => void onCompleted(item, false)}
                  >
                    완료 취소
                  </button>
                ) : (
                  <button
                    type="button"
                    className="button button-quiet"
                    disabled={busyId === item.homeworkId}
                    onClick={() => void onCompleted(item, true)}
                  >
                    완료
                  </button>
                )}
                <button
                  type="button"
                  className="button button-quiet"
                  onClick={() => {
                    setReleaseError(null)
                    setReleaseTarget(item)
                  }}
                >
                  과제 삭제
                </button>
              </div>
            </>
          )}
        </div>
      ))}
      {atLimit ? (
        <p className="form-hint">한 학습 항목에는 활성 과제를 최대 3개까지 등록할 수 있습니다.</p>
      ) : (
        <div className="stack">
          <label htmlFor={`hw-content-${monitoringId}`}>{contentLabel}</label>
          <input id={`hw-content-${monitoringId}`} value={content} onChange={(event) => setContent(event.target.value)} />
          {contentError ? <p className="field-error">{contentError}</p> : null}
          {fieldMessage(createError, 'homeworkContent') ? (
            <p className="field-error">{fieldMessage(createError, 'homeworkContent')}</p>
          ) : null}
          <label htmlFor={`hw-deadline-${monitoringId}`}>{deadlineLabel}</label>
          <input
            id={`hw-deadline-${monitoringId}`}
            type="date"
            value={deadline}
            onChange={(event) => setDeadline(event.target.value)}
          />
          <label htmlFor={`hw-feedback-${monitoringId}`}>{feedbackLabel}</label>
          <input id={`hw-feedback-${monitoringId}`} value={feedback} onChange={(event) => setFeedback(event.target.value)} />
          <button type="button" className="button" disabled={creating || content.trim() === ''} onClick={() => void onCreate()}>
            {creating ? '추가 중' : '과제 추가'}
          </button>
        </div>
      )}
      {createError ? <p className="form-error">{createErrorMessage(createError)}</p> : null}
      {releaseTarget ? (
        <Overlay title="과제 삭제" onClose={() => { if (!releasing) setReleaseTarget(null) }}><div className="overlay-body">
            <p>
              이 과제만 삭제합니다. 학습 기록과 커리큘럼 배정은 유지됩니다. 과제를 다시 활성화하는 화면은 제공하지 않습니다.
            </p>
            {releaseError ? <p className="form-error">{formErrorMessage(releaseError)}</p> : null}
            <div className="modal-actions">
              <button type="button" className="button button-quiet" disabled={releasing} onClick={() => setReleaseTarget(null)}>
                취소
              </button>
              <button type="button" className="button button-danger" disabled={releasing} onClick={() => void onDeactivate()}>
                {releasing ? '처리 중' : '삭제'}
              </button>
            </div>
          </div>
        </Overlay>
      ) : null}
    </div>
  )
}

function toDateInput(value: string | null): string {
  if (!value) {
    return ''
  }
  return value.slice(0, 10)
}

function fromDateInput(value: string): string | null {
  const trimmed = value.trim()
  if (trimmed === '') {
    return null
  }
  return trimmed.length === 10 ? `${trimmed}T00:00:00` : trimmed
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function createErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'COMMON_CONFLICT') {
    return '한 학습 항목에는 활성 과제를 최대 3개까지 등록할 수 있습니다.'
  }
  return formErrorMessage(error) ?? '요청을 처리하지 못했습니다.'
}

function fieldMessage(error: unknown, field: string): string | null {
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
