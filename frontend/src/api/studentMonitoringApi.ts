import type { ProgressStatus } from '../types/student.ts'
import { apiRequest } from './apiClient.ts'

export type MonitoringWriteBody = {
  contentDetailId: number
  currentBpm: number | null
  progressStatus: ProgressStatus
  memo: string | null
}

export type MonitoringPatchBody = {
  currentBpm: number | null
  progressStatus: ProgressStatus
  memo: string | null
}

export type MonitoringCommandResult = {
  monitoringId: number
  contentDetailId: number
  displayOrder: number
  currentBpm: number | null
  progressStatus: ProgressStatus
  memo: string | null
}

export function createMonitoring(studentCurriculumId: number, body: MonitoringWriteBody): Promise<MonitoringCommandResult> {
  return apiRequest<MonitoringCommandResult>(`/v1/student-curriculums/${studentCurriculumId}/monitorings`, {
    method: 'POST',
    body,
  })
}

export function updateMonitoring(
  studentCurriculumId: number,
  monitoringId: number,
  body: MonitoringPatchBody,
): Promise<MonitoringCommandResult> {
  return apiRequest<MonitoringCommandResult>(
    `/v1/student-curriculums/${studentCurriculumId}/monitorings/${monitoringId}`,
    { method: 'PATCH', body },
  )
}

export function deactivateMonitoring(studentCurriculumId: number, monitoringId: number): Promise<void> {
  return apiRequest<void>(`/v1/student-curriculums/${studentCurriculumId}/monitorings/${monitoringId}`, {
    method: 'DELETE',
  })
}
