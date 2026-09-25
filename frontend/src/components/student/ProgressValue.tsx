import type { ProgressSummary } from '../../types/student.ts'
import { formatProgress } from '../../student/display.ts'

export function ProgressValue({ progress }: { progress: ProgressSummary | null }) {
  const label = formatProgress(progress)
  const missing = progress == null
  return <span className={missing ? 'progress-none' : 'progress-value'}>{label}</span>
}
