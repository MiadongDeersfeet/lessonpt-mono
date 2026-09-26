import { useState } from 'react'
import { deleteContentResource, fetchTeacherResourceBlob, uploadContentResource } from '../../api/resourceApi.ts'
import { AUDIO_MAX_BYTES, PDF_MAX_BYTES, formatFileSize } from '../../resource/fileSize.ts'
import { uploadErrorMessage } from '../../resource/uploadError.ts'
import type { ContentDetail, ContentResource } from '../../types/curriculum.ts'

import { ResourceButtons } from './ResourceButtons.tsx'
import { Overlay } from '../layout/Overlay.tsx'

type Props = {
  curriculumId: number
  categoryId: number
  content: ContentDetail
  onContentChange: (content: ContentDetail) => void
}

export function ContentResources({ curriculumId, categoryId, content, onContentChange }: Props) {
  return (
    <div className="resource-panel"><p className="form-hint">파일 업로드·교체·삭제는 즉시 반영됩니다. 내용 편집을 취소해도 파일 변경은 유지됩니다.</p>
      <ResourceSlot
        kind="sheet"
        title="악보"
        emptyLabel="악보 없음"
        accept="application/pdf,.pdf"
        maxBytes={PDF_MAX_BYTES}
        clientLimitMessage="PDF 20MB 초과"
        resource={content.sheet}
        curriculumId={curriculumId}
        categoryId={categoryId}
        content={content}
        onContentChange={onContentChange}
      />
      <ResourceSlot
        kind="audio"
        title="음원"
        emptyLabel="음원 없음"
        accept="audio/mpeg,audio/mp4,audio/x-m4a,.mp3,.m4a"
        maxBytes={AUDIO_MAX_BYTES}
        clientLimitMessage="Audio 50MB 초과"
        resource={content.audio}
        curriculumId={curriculumId}
        categoryId={categoryId}
        content={content}
        onContentChange={onContentChange}
      />
    </div>
  )
}

type SlotProps = Props & {
  kind: 'sheet' | 'audio'
  title: string
  emptyLabel: string
  accept: string
  maxBytes: number
  clientLimitMessage: string
  resource: ContentResource | null
}

function ResourceSlot({
  kind,
  title,
  emptyLabel,
  accept,
  maxBytes,
  clientLimitMessage,
  resource,
  curriculumId,
  categoryId,
  content,
  onContentChange,
}: SlotProps) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [warning, setWarning] = useState('')
  const [confirming, setConfirming] = useState(false)
  const inputId = `${kind}-file-${content.contentDetailId}`

  async function onFile(file: File | undefined) {
    if (!file || busy) {
      return
    }
    if (file.size > maxBytes) {
      setError(clientLimitMessage)
      setWarning('')
      return
    }
    setBusy(true)
    setError('')
    setWarning('')
    try {
      const uploaded = await uploadContentResource(
        curriculumId,
        categoryId,
        content.contentDetailId,
        kind,
        file,
      )
      onContentChange({
        ...content,
        sheet: kind === 'sheet' ? uploaded.resource : content.sheet,
        audio: kind === 'audio' ? uploaded.resource : content.audio,
      })
      if (uploaded.storageWarning) {
        setWarning('저장 공간 사용량이 높습니다.')
      }
    } catch (caught) {
      setError(uploadErrorMessage(caught, kind))
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!resource || busy) {
      return
    }
    setBusy(true)
    setError('')
    try {
      await deleteContentResource(curriculumId, categoryId, content.contentDetailId, resource.resourceId)
      onContentChange({
        ...content,
        sheet: kind === 'sheet' ? null : content.sheet,
        audio: kind === 'audio' ? null : content.audio,
      })
      setConfirming(false)
      setWarning('')
    } catch (caught) {
      setError(uploadErrorMessage(caught, kind))
    } finally {
      setBusy(false)
    }
  }

  async function onDownload() {
    if (!resource || busy) {
      return
    }
    setBusy(true)
    setError('')
    try {
      const blob = await fetchTeacherResourceBlob(
        curriculumId,
        categoryId,
        content.contentDetailId,
        resource.resourceId,
        'attachment',
      )
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = resource.originalFileName
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (caught) {
      setError(uploadErrorMessage(caught, kind))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="resource-slot" aria-label={title}>
      <h3>{title}</h3>
      {resource ? (
        <>
          <p>{resource.originalFileName}</p>
          <p>{formatFileSize(resource.fileSize)}</p>
          <ResourceButtons only={kind} content={content} teacher={{ curriculumId, categoryId, contentDetailId: content.contentDetailId }} />
        </>
      ) : (
        <p className="quiet">{emptyLabel}</p>
      )}
      <div className="row-actions">
        <label htmlFor={inputId}>{resource ? '교체' : kind === 'sheet' ? 'PDF 업로드' : '음원 업로드'}</label>
        <input
          id={inputId}
          type="file"
          accept={accept}
          disabled={busy}
          onChange={(event) => {
            const file = event.target.files?.[0]
            event.target.value = ''
            void onFile(file)
          }}
        />
        {resource ? (
          <>
            <button type="button" className="button button-quiet" disabled={busy} onClick={() => void onDownload()}>
              다운로드
            </button>
            <button type="button" className="button button-danger" disabled={busy} onClick={() => setConfirming(true)}>
              삭제
            </button>
          </>
        ) : null}
      </div>
      {busy ? <p role="status">처리 중</p> : null}
      {warning ? <p className="storage-warning">{warning}</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
      {confirming && resource ? (
        <Overlay title={`${title} 삭제`} onClose={() => { if (!busy) setConfirming(false) }}><div className="overlay-body">
            <p>이 파일을 삭제합니다. 삭제한 파일은 복구할 수 없습니다.</p>
            <div className="modal-actions">
              <button type="button" className="button button-quiet" disabled={busy} onClick={() => setConfirming(false)}>
                취소
              </button>
              <button type="button" className="button button-danger" disabled={busy} onClick={() => void onDelete()}>
                삭제
              </button>
            </div>
          </div>
        </Overlay>
      ) : null}
    </section>
  )
}
