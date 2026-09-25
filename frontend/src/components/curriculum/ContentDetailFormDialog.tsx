import { useState } from 'react'
import type { FormEvent } from 'react'
import { fieldErrorMessage, formErrorMessage } from '../feedback/describeError.ts'
import type { ContentDetail, ContentDetailWriteBody } from '../../types/curriculum.ts'

type Props = {
  content: ContentDetail | null
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
  const [sheetUrl, setSheetUrl] = useState(content?.sheetUrl ?? '')
  const [youtubeUrl, setYoutubeUrl] = useState(content?.youtubeUrl ?? '')
  const [audioUrl, setAudioUrl] = useState(content?.audioUrl ?? '')
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
      sheetUrl: emptyToNull(sheetUrl),
      youtubeUrl: emptyToNull(youtubeUrl),
      audioUrl: emptyToNull(audioUrl),
    })
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <form
        className="modal"
        role="dialog"
        aria-labelledby="content-form-title"
        noValidate
        onMouseDown={(event) => event.stopPropagation()}
        onSubmit={submit}
      >
        <h2 id="content-form-title">{content ? '내용 수정' : '내용 추가'}</h2>
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
        <label htmlFor="content-sheet">악보 URL</label>
        <input id="content-sheet" value={sheetUrl} onChange={(event) => setSheetUrl(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(error, 'sheetUrl')} />
        <label htmlFor="content-youtube">YouTube URL</label>
        <input id="content-youtube" value={youtubeUrl} onChange={(event) => setYoutubeUrl(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(error, 'youtubeUrl')} />
        <label htmlFor="content-audio">오디오 URL</label>
        <input id="content-audio" value={audioUrl} onChange={(event) => setAudioUrl(event.target.value)} />
        <FieldMessage message={fieldErrorMessage(error, 'audioUrl')} />
        {error && !nameError && !bpmError ? <p className="form-error">{formErrorMessage(error)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="submit" className="button" disabled={submitting}>
            {submitting ? '저장 중' : '저장'}
          </button>
        </div>
      </form>
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

function FieldMessage({ message }: { message: string | null }) {
  if (!message) {
    return null
  }
  return <p className="field-error">{message}</p>
}
