export type LoginRequest = {
  email: string
  password: string
}

export type AuthTokenResponse = {
  accessToken: string
  refreshToken: string
  accessTokenExpiresIn: number
}

export type TeacherMe = {
  teacherId: number
  email: string
  name: string
  phone: string | null
}
