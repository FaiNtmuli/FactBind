import type { ApiErrorBody } from '../types/common'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * Error thrown for every non 2xx response. It carries the stable error code of the backend
 * plus the per-field validation messages when the backend rejected a request body.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fields?: Record<string, string>

  constructor(status: number, code: string, message: string, fields?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fields = fields
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  signal?: AbortSignal
}

/**
 * Minimal fetch wrapper: builds the URL, serializes the body as JSON, parses the response and
 * converts non 2xx responses into {@link ApiError}.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, signal } = options

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const payload = parseJson(text)

  if (!response.ok) {
    const errorBody = (payload ?? {}) as Partial<ApiErrorBody>
    throw new ApiError(
      response.status,
      errorBody.code ?? 'UNKNOWN_ERROR',
      errorBody.message ?? `Request failed with status ${response.status}`,
      errorBody.fields,
    )
  }

  return payload as T
}

function parseJson(text: string): unknown {
  if (!text) {
    return undefined
  }
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

export type QueryValue = string | number | boolean | undefined | null

/**
 * Builds a query string, skipping parameters that are empty.
 */
export function buildQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    search.set(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}
