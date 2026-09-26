import { formErrorMessage } from '../feedback/describeError.ts'

type Props = {
  name: string
  submitting: boolean
  error: unknown
  onClose: () => void
  onConfirm: () => void
}

export function ReleaseLocationDialog({ name, submitting, error, onClose, onConfirm }: Props) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-labelledby="release-location-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h2 id="release-location-title">출강처 삭제</h2>
        <p>
          {name}을 삭제합니다. 이 장소의 학생 연결과 그 활성 수강도 함께 해제됩니다. 모니터링과 과제 기록은
          남고, 장소를 다시 활성화해도 학생 연결과 수강은 자동으로 돌아오지 않습니다.
        </p>
        {error ? <p className="form-error">{formErrorMessage(error)}</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className="button button-danger" onClick={onConfirm} disabled={submitting}>
            {submitting ? '처리 중' : '삭제'}
          </button>
        </div>
      </div>
    </div>
  )
}
