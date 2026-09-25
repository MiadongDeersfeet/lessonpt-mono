import type { StudentCreateBody, StudentLearningDetail, StudentSummary, StudentUpdateBody } from '../types/student.ts'
import { apiRequest } from './apiClient.ts'

export function listStudents(): Promise<StudentSummary[]> {
  return apiRequest<StudentSummary[]>('/v1/students')
}

export function getStudent(studentId: number): Promise<StudentSummary> {
  return apiRequest<StudentSummary>(`/v1/students/${studentId}`)
}

export function getStudentLearning(studentId: number): Promise<StudentLearningDetail> {
  return apiRequest<StudentLearningDetail>(`/v1/students/${studentId}/learning`)
}

export function createStudent(body: StudentCreateBody): Promise<StudentSummary> {
  return apiRequest<StudentSummary>('/v1/students', { method: 'POST', body })
}

export function updateStudent(studentId: number, body: StudentUpdateBody): Promise<StudentSummary> {
  return apiRequest<StudentSummary>(`/v1/students/${studentId}`, { method: 'PATCH', body })
}

export function releaseStudent(studentId: number): Promise<void> {
  return apiRequest<void>(`/v1/students/${studentId}`, { method: 'DELETE' })
}
