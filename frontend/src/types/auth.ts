export type LoginRequest = {
  email: string
  password: string
}

export type SignupRequest = {
  email: string
  password: string
  name: string
}

export type TeacherSignup = {
  teacherId: number
  email: string
  name: string
  phone: string | null
  role: string
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
