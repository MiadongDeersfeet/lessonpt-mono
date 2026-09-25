type Props = {
  name: string
  submitting: boolean
  error: unknown
  onClose: () => void
  onConfirm: () => void
}

export function ReleaseStudentDialog({ name, submitting, error, onClose, onConfirm }: Props) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-labelledby="release-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <h2 id="release-title">연결 해제</h2>
        <p>
          {name}님과의 연결을 해제합니다. 학생 기록은 남고, 이 강사와의 관계와 출강처·수강 연결만 비활성화됩니다.
        </p>
        {error ? <p className="form-error">연결을 해제하지 못했습니다. 잠시 후 다시 시도해 주세요.</p> : null}
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose} disabled={submitting}>
            취소
          </button>
          <button type="button" className="button button-danger" onClick={onConfirm} disabled={submitting}>
            {submitting ? '해제 중' : '연결 해제'}
          </button>
        </div>
      </div>
    </div>
  )
}
