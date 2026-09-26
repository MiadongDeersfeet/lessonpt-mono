import { formErrorMessage } from '../feedback/describeError.ts'

type Props = {
  title: string
  message: string
  submitting: boolean
  error: unknown
  confirmLabel?: string
  onClose: () => void
  onConfirm: () => void
}

export function DeactivateDialog({ title, message, submitting, error, confirmLabel = '비활성화', onClose, onConfirm }: Props) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-labelledby="deactivate-title" onMouseDown={(event) => event.stopPropagation()}>
        <h2 id="deactivate-title">{title}</h2>
        <p>{message}</p>
        {error ? <p className="form-error">{formErrorMessage(error)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className="button button-danger" onClick={onConfirm} disabled={submitting}>
            {submitting ? '처리 중' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
