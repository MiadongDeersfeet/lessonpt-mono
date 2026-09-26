import type { ContentResource } from './curriculum.ts'
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
  homeworkId: number
  content: string
  deadline: string | null
  completed: boolean | null
  feedback: string | null
}

export type StudentPortalContent = {
  monitoringId: number
  name: string
  targetBpm: number | null
  currentBpm: number | null
  progressStatus: ProgressStatus | null
  youtubeUrl: string | null
  sheet: ContentResource | null
  audio: ContentResource | null
  homeworks: StudentPortalHomework[]
}

export type StudentPortalCategory = {
  name: string
  totalContentCount: number
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
