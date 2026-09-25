import type { StudentCurriculumAssignBody, StudentCurriculumAssignment } from '../types/studentCurriculum.ts'
import { apiRequest } from './apiClient.ts'

export function assignStudentCurriculum(
  teacherStudentLocationId: number,
  body: StudentCurriculumAssignBody,
): Promise<StudentCurriculumAssignment> {
  return apiRequest<StudentCurriculumAssignment>(`/v1/student-locations/${teacherStudentLocationId}/curriculums`, {
    method: 'POST',
    body,
  })
}

export function updateStudentCurriculumMemo(
  teacherStudentLocationId: number,
  studentCurriculumId: number,
  memo: string | null,
): Promise<StudentCurriculumAssignment> {
  return apiRequest<StudentCurriculumAssignment>(
    `/v1/student-locations/${teacherStudentLocationId}/curriculums/${studentCurriculumId}`,
    { method: 'PATCH', body: { memo } },
  )
}

export function releaseStudentCurriculum(teacherStudentLocationId: number, studentCurriculumId: number): Promise<void> {
  return apiRequest<void>(`/v1/student-locations/${teacherStudentLocationId}/curriculums/${studentCurriculumId}`, {
    method: 'DELETE',
  })
}
