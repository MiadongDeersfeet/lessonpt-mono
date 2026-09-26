import { useState } from 'react'
import type { FormEvent } from 'react'
import { fieldErrorMessage, formErrorMessage } from '../feedback/describeError.ts'
import type { ContentDetail, ContentDetailWriteBody } from '../../types/curriculum.ts'
import { Overlay } from '../layout/Overlay.tsx'

type Props = {
  content: ContentDetail
  submitting: boolean
  error: unknown
  onClose: () => void
  onSubmit: (body: ContentDetailWriteBody) => void
}

export function ContentDetailFormDialog({ content, submitting, error, onClose, onSubmit }: Props) {
  const [name, setName] = useState(content?.name ?? '')
  const [targetBpm, setTargetBpm] = useState(content?.targetBpm == null ? '' : String(content.targetBpm))
  const [memo, setMemo] = useState(content?.memo ?? '')
  const [evaluationMemo, setEvaluationMemo] = useState(content?.evaluationMemo ?? '')
  const [youtubeUrl, setYoutubeUrl] = useState(content.youtubeUrl ?? '')
  const [nameError, setNameError] = useState('')
  const [bpmError, setBpmError] = useState('')

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedName = name.trim()
    const bpm = parseBpm(targetBpm)
    const nextNameError = trimmedName === '' ? '이름은 필수입니다.' : ''
    const nextBpmError = bpm === 'invalid' ? '목표 BPM은 60 이상 240 이하여야 합니다.' : ''
    setNameError(nextNameError)
    setBpmError(nextBpmError)
    if (nextNameError || nextBpmError || bpm === 'invalid') {
      return
    }
    onSubmit({
      name: trimmedName,
      memo: emptyToNull(memo),
      targetBpm: bpm,
      evaluationMemo: emptyToNull(evaluationMemo),
      youtubeUrl: emptyToNull(youtubeUrl),
    })
  }

  return (
    <Overlay title={content ? '내용 수정' : '내용 추가'} className="content-editor" onClose={() => { if (!submitting) onClose() }}>
      <form
        className="content-editor-form"
        noValidate
        onMouseDown={(event) => event.stopPropagation()}
        onSubmit={submit}
      >
        <div className="overlay-body editor-fields">
        <label htmlFor="content-name">이름</label>
        <input id="content-name" value={name} onChange={(event) => setName(event.target.value)} />
        <FieldMessage message={nameError || fieldErrorMessage(error, 'name')} />
        <label htmlFor="content-bpm">목표 BPM</label>
        <input id="content-bpm" inputMode="numeric" value={targetBpm} onChange={(event) => setTargetBpm(event.target.value)} />
        <FieldMessage message={bpmError || fieldErrorMessage(error, 'targetBpm')} />
        <label htmlFor="content-memo">메모</label>
        <textarea id="content-memo" value={memo} onChange={(event) => setMemo(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(error, 'memo')} />
        <label htmlFor="content-evaluation">평가 메모</label>
        <textarea id="content-evaluation" value={evaluationMemo} onChange={(event) => setEvaluationMemo(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(error, 'evaluationMemo')} />
        <label htmlFor="content-youtube">YouTube URL</label>
        <input id="content-youtube" type="url" inputMode="url" value={youtubeUrl} onChange={(event) => setYoutubeUrl(withoutLeadingSpace(event.target.value))} />
        <FieldMessage message={fieldErrorMessage(error, 'youtubeUrl')} />
        {error && !nameError && !bpmError ? <p className="form-error">{formErrorMessage(error)}</p> : null}
        </div>
        <div className="modal-actions overlay-footer">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? '저장 중' : '저장'}
          </button>
        </div>
      </form>
    </Overlay>
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

function withoutLeadingSpace(value: string): string {
  return value.replace(/^\s+/, '')
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function FieldMessage({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return <p className="field-error">{message}</p>
}
