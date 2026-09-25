import type { Homework } from '../types/student.ts'
import { apiRequest } from './apiClient.ts'

export type HomeworkWriteBody = {
  homeworkContent: string
  deadline: string | null
  feedback: string | null
}

export type HomeworkPatchBody = {
  homeworkContent?: string
  deadline?: string | null
  completed?: boolean
  feedback?: string | null
}

export function createHomework(monitoringId: number, body: HomeworkWriteBody): Promise<Homework> {
  return apiRequest<Homework>(`/v1/monitorings/${monitoringId}/homeworks`, {
    method: 'POST',
    body,
  })
}

export function updateHomework(monitoringId: number, homeworkId: number, body: HomeworkPatchBody): Promise<Homework> {
  return apiRequest<Homework>(`/v1/monitorings/${monitoringId}/homeworks/${homeworkId}`, {
    method: 'PATCH',
    body,
  })
}

export function deactivateHomework(monitoringId: number, homeworkId: number): Promise<void> {
  return apiRequest<void>(`/v1/monitorings/${monitoringId}/homeworks/${homeworkId}`, {
    method: 'DELETE',
  })
}
