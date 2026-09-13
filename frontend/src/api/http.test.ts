import { describe, expect, it } from 'vitest'
import { ApiError, buildQuery, request } from './http'
import { mockFetch } from '../test/mockFetch'

describe('request', () => {
  it('parses a JSON response', async () => {
    mockFetch(() => ({ body: { id: 7, name: 'Alice Anderson' } }))

    const result = await request<{ id: number; name: string }>('/api/users/7')

    expect(result).toEqual({ id: 7, name: 'Alice Anderson' })
  })

  it('returns undefined for a 204 response', async () => {
    const calls = mockFetch(() => ({ status: 204 }))

    const result = await request<void>('/api/users/7', { method: 'DELETE' })

    expect(result).toBeUndefined()
    expect(calls[0]).toMatchObject({ url: '/api/users/7', method: 'DELETE' })
  })

  it('serializes the request body as JSON', async () => {
    const calls = mockFetch(() => ({ status: 201, body: { id: 1 } }))

    await request('/api/users', { method: 'POST', body: { name: 'Alice' } })

    expect(calls[0]).toMatchObject({ method: 'POST', body: { name: 'Alice' } })
  })

  it('turns an error response into an ApiError with code and fields', async () => {
    mockFetch(() => ({
      status: 400,
      body: { code: 'VALIDATION_ERROR', message: 'Validation failed', fields: { email: 'must be an email' } },
    }))

    const promise = request('/api/users', { method: 'POST', body: {} })

    await expect(promise).rejects.toBeInstanceOf(ApiError)
    await promise.catch((error: ApiError) => {
      expect(error.status).toBe(400)
      expect(error.code).toBe('VALIDATION_ERROR')
      expect(error.fields).toEqual({ email: 'must be an email' })
    })
  })

  it('falls back to a generic error when the body is not JSON', async () => {
    mockFetch(() => ({ status: 500 }))

    await expect(request('/api/users')).rejects.toMatchObject({ code: 'UNKNOWN_ERROR', status: 500 })
  })
})

describe('buildQuery', () => {
  it('skips empty values and keeps the others', () => {
    expect(buildQuery({ keyword: 'tom', status: undefined, page: 0, size: 20 })).toBe(
      '?keyword=tom&page=0&size=20',
    )
  })

  it('returns an empty string when nothing is set', () => {
    expect(buildQuery({ keyword: '', status: undefined })).toBe('')
  })
})
