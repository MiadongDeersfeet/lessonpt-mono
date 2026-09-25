import type { ContentResource } from '../types/curriculum.ts'
import { ApiError, apiSend } from './apiClient.ts'

export type UploadedResource = {
  resource: ContentResource
  storageWarning: boolean
}

export function uploadContentResource(
  curriculumId: number,
  categoryId: number,
  contentDetailId: number,
  resourceType: 'sheet' | 'audio',
  file: File,
): Promise<UploadedResource> {
  const body = new FormData()
  body.append('file', file)
  return apiSend(resourcePath(curriculumId, categoryId, contentDetailId, resourceType), {
    method: 'POST',
    body,
  }).then(async (response) => ({
    resource: (await response.json()) as ContentResource,
    storageWarning: response.headers.get('X-Lessonpt-Storage-Warning') === 'true',
  }))
}

export function deleteContentResource(
  curriculumId: number,
  categoryId: number,
  contentDetailId: number,
  resourceId: number,
): Promise<void> {
  return apiSend(`${resourceBase(curriculumId, categoryId, contentDetailId)}/${resourceId}`, {
    method: 'DELETE',
  }).then(() => undefined)
}

export async function fetchTeacherResourceBlob(
  curriculumId: number,
  categoryId: number,
  contentDetailId: number,
  resourceId: number,
  disposition: 'inline' | 'attachment',
): Promise<Blob> {
  const response = await apiSend(
    `${resourceBase(curriculumId, categoryId, contentDetailId)}/${resourceId}/content?disposition=${disposition}`,
  )
  return response.blob()
}

export function studentResourceContentUrl(resourceId: number, disposition: 'inline' | 'attachment'): string {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')
  return `${base}/v1/student/resources/${resourceId}/content?disposition=${disposition}`
}

export async function fetchStudentResourceBlob(
  resourceId: number,
  disposition: 'inline' | 'attachment',
): Promise<Blob> {
  let response: Response
  try {
    response = await fetch(studentResourceContentUrl(resourceId, disposition), {
      credentials: 'include',
      headers: { Accept: 'application/pdf' },
    })
  } catch {
    throw new ApiError(0, 'NETWORK', '서버에 연결하지 못했습니다.', null, [])
  }
  if (!response.ok) {
    throw new ApiError(response.status, 'UNKNOWN', '악보를 불러오지 못했습니다.', null, [])
  }
  return response.blob()
}

function resourceBase(curriculumId: number, categoryId: number, contentDetailId: number): string {
  return `/v1/curriculums/${curriculumId}/categories/${categoryId}/content-details/${contentDetailId}/resources`
}

function resourcePath(
  curriculumId: number,
  categoryId: number,
  contentDetailId: number,
  resourceType: 'sheet' | 'audio',
): string {
  return `${resourceBase(curriculumId, categoryId, contentDetailId)}/${resourceType}`
}
