import { useId, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { createContentDetail } from '../../api/curriculumApi.ts'
import { fieldErrorMessage, formErrorMessage } from '../feedback/describeError.ts'
import type { ContentDetail } from '../../types/curriculum.ts'

export function QuickContentForm({ curriculumId, categoryId, onCreated, onClose }: {
  curriculumId: number
  categoryId: number
  onCreated: (content: ContentDetail) => void
  onClose: () => void
}) {
  const formId = useId().replace(/:/g, '')
  const input = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [bpm, setBpm] = useState('')
  const [youtubeUrl, setYoutubeUrl] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [validation, setValidation] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) {
      return
    }
    const value = bpm.trim() === '' ? null : Number(bpm)
    if (!name.trim()) {
      setValidation('이름은 필수입니다.')
      return
    }
    if (value != null && (!/^\d+$/.test(bpm.trim()) || value < 60 || value > 240)) {
      setValidation('목표 BPM은 60 이상 240 이하여야 합니다.')
      return
    }
    setValidation('')
    setError(null)
    setBusy(true)
    try {
      const created = await createContentDetail(curriculumId, categoryId, {
        name: name.trim(),
        targetBpm: value,
        memo: null,
        evaluationMemo: null,
        youtubeUrl: emptyToNull(youtubeUrl),
      })
      onCreated(created)
      setName('')
      setBpm('')
      setYoutubeUrl('')
      input.current?.focus()
    } catch (caught) {
      setError(caught)
    } finally {
      setBusy(false)
    }
  }

  const message = validation || fieldErrorMessage(error, 'name') || fieldErrorMessage(error, 'targetBpm') || fieldErrorMessage(error, 'youtubeUrl') || (error ? formErrorMessage(error) : null)
  return (
    <>
      <form id={formId} className="sr-only" noValidate onSubmit={(event) => void submit(event)} />
      <tr className="content-draft">
        <td data-label="순서" />
        <td data-label="이름">
          <label className="sr-only" htmlFor={`${formId}-name`}>이름</label>
          <input ref={input} id={`${formId}-name`} form={formId} value={name} onChange={(event) => setName(event.target.value)} aria-describedby={message ? `${formId}-error` : undefined} />
        </td>
        <td data-label="목표 BPM">
          <label className="sr-only" htmlFor={`${formId}-bpm`}>목표 BPM</label>
          <input id={`${formId}-bpm`} form={formId} inputMode="numeric" placeholder="선택" value={bpm} onChange={(event) => setBpm(event.target.value)} />
        </td>
        <td data-label="PDF"><span className="quiet">추가 후</span></td>
        <td data-label="영상">
          <label className="sr-only" htmlFor={`${formId}-youtube`}>YouTube URL</label>
          <input
            id={`${formId}-youtube`}
            form={formId}
            type="url"
            inputMode="url"
            placeholder="https://"
            value={youtubeUrl}
            onChange={(event) => setYoutubeUrl(withoutLeadingSpace(event.target.value))}
          />
        </td>
        <td data-label="음원"><span className="quiet">추가 후</span></td>
        <td data-label="작업">
          <div className="row-actions">
            <button className="button" type="submit" form={formId} disabled={busy}>{busy ? '추가 중' : '추가'}</button>
            <button className="button button-quiet" type="button" disabled={busy} onClick={onClose}>취소</button>
          </div>
        </td>
      </tr>
      {message ? (
        <tr>
          <td colSpan={7}>
            <p className="form-error" id={`${formId}-error`} role="alert">{message}</p>
          </td>
        </tr>
      ) : null}
    </>
  )
}

function withoutLeadingSpace(value: string): string {
  return value.replace(/^\s+/, '')
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}
