import type { StudentLearningDetail, StudentSummary } from '../types/student.ts'
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
