import type { Category, ContentDetail, ContentDetailWriteBody, Curriculum } from '../types/curriculum.ts'
import { apiRequest } from './apiClient.ts'

export function listCurriculums(): Promise<Curriculum[]> {
  return apiRequest<Curriculum[]>('/v1/curriculums')
}

export function getCurriculum(curriculumId: number): Promise<Curriculum> {
  return apiRequest<Curriculum>(`/v1/curriculums/${curriculumId}`)
}

export function createCurriculum(name: string): Promise<Curriculum> {
  return apiRequest<Curriculum>('/v1/curriculums', { method: 'POST', body: { name } })
}

export function updateCurriculum(curriculumId: number, name: string): Promise<Curriculum> {
  return apiRequest<Curriculum>(`/v1/curriculums/${curriculumId}`, { method: 'PATCH', body: { name } })
}

export function deactivateCurriculum(curriculumId: number): Promise<void> {
  return apiRequest<void>(`/v1/curriculums/${curriculumId}`, { method: 'DELETE' })
}

export function listCategories(curriculumId: number): Promise<Category[]> {
  return apiRequest<Category[]>(`/v1/curriculums/${curriculumId}/categories`)
}

export function createCategory(curriculumId: number, name: string): Promise<Category> {
  return apiRequest<Category>(`/v1/curriculums/${curriculumId}/categories`, { method: 'POST', body: { name } })
}

export function updateCategory(curriculumId: number, categoryId: number, name: string): Promise<Category> {
  return apiRequest<Category>(`/v1/curriculums/${curriculumId}/categories/${categoryId}`, {
    method: 'PATCH',
    body: { name },
  })
}

export function deactivateCategory(curriculumId: number, categoryId: number): Promise<void> {
  return apiRequest<void>(`/v1/curriculums/${curriculumId}/categories/${categoryId}`, { method: 'DELETE' })
}

export function listContentDetails(curriculumId: number, categoryId: number): Promise<ContentDetail[]> {
  return apiRequest<ContentDetail[]>(
    `/v1/curriculums/${curriculumId}/categories/${categoryId}/content-details`,
  )
}

export function createContentDetail(
  curriculumId: number,
  categoryId: number,
  body: ContentDetailWriteBody,
): Promise<ContentDetail> {
  return apiRequest<ContentDetail>(`/v1/curriculums/${curriculumId}/categories/${categoryId}/content-details`, {
    method: 'POST',
    body,
  })
}

export function updateContentDetail(
  curriculumId: number,
  categoryId: number,
  contentDetailId: number,
  body: ContentDetailWriteBody,
): Promise<ContentDetail> {
  return apiRequest<ContentDetail>(
    `/v1/curriculums/${curriculumId}/categories/${categoryId}/content-details/${contentDetailId}`,
    { method: 'PATCH', body },
  )
}

export function deactivateContentDetail(
  curriculumId: number,
  categoryId: number,
  contentDetailId: number,
): Promise<void> {
  return apiRequest<void>(
    `/v1/curriculums/${curriculumId}/categories/${categoryId}/content-details/${contentDetailId}`,
    { method: 'DELETE' },
  )
}
