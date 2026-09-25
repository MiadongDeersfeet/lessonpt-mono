export type FieldError = {
  field: string
  message: string
}

export type ErrorResponse = {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  traceId: string
  fieldErrors: FieldError[]
}
