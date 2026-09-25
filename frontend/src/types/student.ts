export type ProgressStatus = 'YET' | 'IN_PROGRESS' | 'COMPLETED' | 'STOPPED'

export type ProgressSummary = {
  completedCount: number
  totalCount: number
  percentage: number
}

export type StudentSummary = {
  studentId: number
  email: string | null
  name: string
  phone: string | null
  teacherStudentId: number
}

export type StudentCreateBody = {
  name: string
  email: string | null
  phone: string | null
}

export type StudentUpdateBody = {
  name: string
  email: string | null
  phone: string | null
}

export type Homework = {
  homeworkId: number
  homeworkContent: string
  deadline: string | null
  completed: boolean | null
  feedback: string | null
}

export type Monitoring = {
  monitoringId: number
  contentDetailId: number
  contentDetailName: string
  displayOrder: number | null
  targetBpm: number | null
  currentBpm: number | null
  progressStatus: ProgressStatus
  memo: string | null
  homeworks: Homework[]
}

export type StudentCurriculum = {
  studentCurriculumId: number
  curriculumId: number
  curriculumName: string
  reenrolled: boolean | null
  memo: string | null
  progress: ProgressSummary | null
  monitorings: Monitoring[]
}

export type LearningLocation = {
  teacherStudentLocationId: number
  locationId: number
  locationName: string
  address: string | null
  studentCurriculums: StudentCurriculum[]
}

export type StudentAccess = {
  teacherStudentAccessId: number
  teacherStudentId: number
  publicAccessKey: string
  createdAt: string | null
  status: 'ACTIVE' | 'INACTIVE'
}

export type StudentLearningDetail = {
  studentId: number
  name: string
  email: string | null
  phone: string | null
  locations: LearningLocation[]
}
