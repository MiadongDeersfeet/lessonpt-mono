import type { Location, LocationWriteBody, StudentLocationAssignment } from '../types/location.ts'
import { apiRequest } from './apiClient.ts'

export function listLocations(): Promise<Location[]> {
  return apiRequest<Location[]>('/v1/locations')
}

export function createLocation(body: LocationWriteBody): Promise<Location> {
  return apiRequest<Location>('/v1/locations', { method: 'POST', body })
}

export function updateLocation(locationId: number, body: LocationWriteBody): Promise<Location> {
  return apiRequest<Location>(`/v1/locations/${locationId}`, { method: 'PATCH', body })
}

export function deactivateLocation(locationId: number): Promise<void> {
  return apiRequest<void>(`/v1/locations/${locationId}`, { method: 'DELETE' })
}

export function assignStudentLocation(studentId: number, locationId: number): Promise<StudentLocationAssignment> {
  return apiRequest<StudentLocationAssignment>(`/v1/students/${studentId}/locations/${locationId}`, { method: 'POST' })
}

export function releaseStudentLocation(studentId: number, locationId: number): Promise<void> {
  return apiRequest<void>(`/v1/students/${studentId}/locations/${locationId}`, { method: 'DELETE' })
}
