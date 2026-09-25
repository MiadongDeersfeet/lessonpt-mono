import type { ProgressStatus, ProgressSummary } from '../types/student.ts'

export function formatProgress(progress: ProgressSummary | null): string {
  if (progress == null) {
    return '진행률 계산 대상 없음'
  }
  return `${progress.percentage.toFixed(1)}%`
}

export function progressStatusLabel(status: ProgressStatus): string {
  switch (status) {
    case 'YET':
      return '시작 전'
    case 'IN_PROGRESS':
      return '진행 중'
    case 'COMPLETED':
      return '완료'
    case 'STOPPED':
      return '중단'
  }
}

export function formatBpm(value: number | null): string {
  return value == null ? '-' : String(value)
}

export function formatDeadline(value: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

export function textOrDash(value: string | null): string {
  return value == null || value.trim() === '' ? '-' : value
}
