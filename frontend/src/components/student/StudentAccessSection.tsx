import { useEffect, useState } from 'react'
import { ApiError } from '../../api/apiClient.ts'
import { createStudentAccess, deleteStudentAccess, getStudentAccess } from '../../api/studentApi.ts'
import { DeactivateDialog } from '../curriculum/DeactivateDialog.tsx'
import { formErrorMessage } from '../feedback/describeError.ts'
import { LoadingState } from '../feedback/LoadingState.tsx'
import type { StudentAccess } from '../../types/student.ts'

export function StudentAccessSection({ studentId, email }: { studentId: number; email: string | null }) {
  const [access, setAccess] = useState<StudentAccess | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<unknown>(null)
  const [actionError, setActionError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const missingEmail = email == null || email.trim() === ''

  useEffect(() => {
    let active = true
    setLoading(true)
    setLoadError(null)
    setActionError(null)
    getStudentAccess(studentId)
      .then((next) => {
        if (active) {
          setAccess(next)
        }
      })
      .catch((caught) => {
        if (!active) {
          return
        }
        if (isNoActiveAccess(caught)) {
          setAccess(null)
          return
        }
        setLoadError(caught)
      })
      .finally(() => {
        if (active) {
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [studentId])

  async function onOpen() {
    setSubmitting(true)
    setActionError(null)
    setNotice(null)
    try {
      setAccess(await createStudentAccess(studentId))
    } catch (caught) {
      setActionError(caught)
    } finally {
      setSubmitting(false)
    }
  }

  async function onRevoke() {
    setSubmitting(true)
    setActionError(null)
    try {
      await deleteStudentAccess(studentId)
      setAccess(null)
      setNotice(null)
      setConfirming(false)
    } catch (caught) {
      setActionError(caught)
    } finally {
      setSubmitting(false)
    }
  }

  async function onCopy(invitationUrl: string) {
    try {
      await navigator.clipboard.writeText(invitationUrl)
      setNotice('복사되었습니다.')
    } catch {
      setNotice('복사에 실패했습니다. 링크를 직접 선택해 주세요.')
    }
  }

  if (loading) {
    return <LoadingState label="접근 권한을 확인하는 중" />
  }
  if (loadError) {
    return <p className="form-error">{formErrorMessage(loadError)}</p>
  }

  const invitationUrl = access ? `${window.location.origin}/student/access/${access.publicAccessKey}` : ''

  return (
    <section className="card stack">
      <h2>학생 포털 접근</h2>
      {access ? (
        <>
          <p>상태: 활성</p>
          <p className="quiet">초대 링크</p>
          <p className="invitation-link">{invitationUrl}</p>
          <div className="modal-actions">
            <button type="button" className="button" disabled={submitting} onClick={() => void onCopy(invitationUrl)}>
              초대 링크 복사
            </button>
            <button type="button" className="button button-danger" disabled={submitting} onClick={() => setConfirming(true)}>
              접근 해제
            </button>
          </div>
        </>
      ) : (
        <>
          <p>상태: 접근 권한 없음</p>
          <p>학생이 포털을 사용하려면 접근 권한을 열어 주세요.</p>
          {missingEmail ? <p className="form-error">학생 이메일을 먼저 등록해 주세요.</p> : null}
          <button type="button" className="button" disabled={submitting || missingEmail} onClick={() => void onOpen()}>
            접근 권한 열기
          </button>
        </>
      )}
      {notice ? <p className="form-hint">{notice}</p> : null}
      {actionError && !confirming ? <p className="form-error">{accessActionMessage(actionError)}</p> : null}
      {confirming ? (
        <DeactivateDialog
          title="접근 해제"
          confirmLabel="접근 해제"
          message="학생 포털 접근이 중지됩니다. 현재 이 접근권한으로 로그인된 학생 세션도 종료됩니다. 다시 열면 새 초대 링크가 생성됩니다."
          submitting={submitting}
          error={actionError}
          onClose={() => {
            if (!submitting) {
              setConfirming(false)
            }
          }}
          onConfirm={() => void onRevoke()}
        />
      ) : null}
    </section>
  )
}

function isNoActiveAccess(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404 && error.code === 'COMMON_NOT_FOUND'
}

function accessActionMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 409 && error.message) {
    return error.message
  }
  if (error instanceof ApiError && error.status === 404) {
    return '활성 학생 관계가 없어 접근 권한을 열 수 없습니다.'
  }
  return formErrorMessage(error) ?? '요청을 처리하지 못했습니다.'
}
