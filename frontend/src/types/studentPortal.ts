import type { ProgressStatus, ProgressSummary } from './student.ts'

export type StudentRelationshipLocation = {
  locationId: number
  locationName: string
}

export type StudentRelationship = {
  teacherStudentAccessId: number
  teacherStudentId: number
  teacherName: string
  locations: StudentRelationshipLocation[]
}

export type StudentMe = {
  name: string
}

export type StudentPortalHomework = {
  content: string
  deadline: string | null
  completed: boolean | null
  feedback: string | null
}

export type StudentPortalContent = {
  name: string
  targetBpm: number | null
  currentBpm: number | null
  progressStatus: ProgressStatus | null
  sheetUrl: string | null
  youtubeUrl: string | null
  audioUrl: string | null
  homeworks: StudentPortalHomework[]
}

export type StudentPortalCategory = {
  name: string
  contents: StudentPortalContent[]
}

export type StudentPortalCurriculum = {
  name: string
  progress: ProgressSummary | null
  categories: StudentPortalCategory[]
}

export type StudentPortalLearning = {
  curriculums: StudentPortalCurriculum[]
}
