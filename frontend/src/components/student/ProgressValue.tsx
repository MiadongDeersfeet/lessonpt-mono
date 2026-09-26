import type { ProgressSummary } from '../../types/student.ts'
import { formatProgress } from '../../student/display.ts'

export function ProgressValue({ progress, meter = false }: { progress: ProgressSummary | null; meter?: boolean }) {
  const label = formatProgress(progress)
  if (progress == null) {
    return <span className="progress-none">{label}</span>
  }
  if (!meter) {
    return <span className="progress-value">{label}</span>
  }
  const width = Math.min(100, Math.max(0, progress.percentage))
  return (
    <span className="progress-meter">
      <span className="progress-meter-value">{label}</span>
      <span className="progress-meter-track" aria-hidden="true">
        <span className="progress-meter-fill" style={{ width: `${width}%` }} />
      </span>
    </span>
  )
}
