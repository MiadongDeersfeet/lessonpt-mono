import type { StudentLearningDetail } from '../types/student.ts'
import { formatProgress } from './display.ts'

export type StudentListSummary = {
  locations: string
  curricula: string
  progressLabel: string
  portalLabel: string
  hasMemo: boolean
}

export function summarizeStudent(detail: StudentLearningDetail | null, portalActive: boolean | null): StudentListSummary {
  const curricula = detail?.locations.flatMap((location) => location.studentCurriculums) ?? []
  const locationNames = detail?.locations.map((location) => location.locationName) ?? []
  const progress = curricula.find((item) => item.progress != null)?.progress ?? null
  const hasMemo = curricula.some(
    (item) => hasText(item.memo) || item.monitorings.some((monitoring) => hasText(monitoring.memo)),
  )
  return {
    locations: locationNames.length > 0 ? locationNames.join(', ') : '출강처 없음',
    curricula: curricula.length > 0 ? curricula.map((item) => item.curriculumName).join(', ') : '커리큘럼 없음',
    progressLabel: progress ? `진행 ${formatProgress(progress)}` : '진행 없음',
    portalLabel: portalActive == null ? 'Portal 확인 실패' : portalActive ? 'Portal 활성' : 'Portal 없음',
    hasMemo,
  }
}

function hasText(value: string | null): boolean {
  return value != null && value.trim() !== ''
}
