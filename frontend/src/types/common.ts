/**
 * Pagination envelope returned by every list endpoint.
 */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

/**
 * Error body returned by the backend for every failing request.
 */
export interface ApiErrorBody {
  code: string
  message: string
  fields?: Record<string, string>
}
