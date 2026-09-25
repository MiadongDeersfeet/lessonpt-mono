export type Curriculum = {
  curriculumId: number
  name: string
  displayOrder: number
}

export type Category = {
  categoryId: number
  name: string
  displayOrder: number
}

export type ContentResource = {
  resourceId: number
  resourceType: 'SHEET' | 'AUDIO'
  originalFileName: string
  contentType: string
  fileSize: number
  createdAt: string
}

export type ContentDetail = {
  contentDetailId: number
  name: string
  displayOrder: number
  memo: string | null
  targetBpm: number | null
  evaluationMemo: string | null
  sheetUrl: string | null
  youtubeUrl: string | null
  audioUrl: string | null
  sheet: ContentResource | null
  audio: ContentResource | null
}

export type ContentDetailWriteBody = {
  name: string
  memo: string | null
  targetBpm: number | null
  evaluationMemo: string | null
  youtubeUrl: string | null
}
