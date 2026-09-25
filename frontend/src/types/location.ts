export type Location = {
  locationId: number
  name: string
  displayOrder: number
  address: string | null
  createdAt: string
  updatedAt: string
}

export type LocationWriteBody = {
  name: string
  address: string | null
}

export type StudentLocationAssignment = {
  teacherStudentLocationId: number
  locationId: number
  locationName: string
  address: string | null
}
