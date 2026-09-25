export type StudentCurriculumAssignment = {
  studentCurriculumId: number
  curriculumId: number
  reenrolled: boolean | null
  memo: string | null
}

export type StudentCurriculumAssignBody = {
  curriculumId: number
  memo: string | null
}
