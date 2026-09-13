import { vi } from 'vitest'

export interface MockResponse {
  status?: number
  body?: unknown
}

export interface FetchCall {
  url: string
  method: string
  body: unknown
}

/**
 * Installs a fake fetch implementation and records every call, so that tests can assert both
 * the rendered result and the HTTP requests that were sent.
 */
export function mockFetch(handler: (call: FetchCall) => MockResponse) {
  const calls: FetchCall[] = []

  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString()
    const call: FetchCall = {
      url,
      method: init?.method ?? 'GET',
      body: init?.body ? JSON.parse(String(init.body)) : undefined,
    }
    calls.push(call)

    const { status = 200, body } = handler(call)
    const payload = body === undefined ? null : JSON.stringify(body)
    return new Response(status === 204 ? null : payload, {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  })

  vi.stubGlobal('fetch', fetchMock)
  return calls
}

export function pageResponse<T>(content: T[], overrides: Partial<Record<string, unknown>> = {}) {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
    ...overrides,
  }
}

export function user(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 1,
    name: 'Alice Anderson',
    email: 'alice@example.com',
    age: 30,
    status: 'ACTIVE',
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-01-01T10:00:00Z',
    ...overrides,
  }
}

export function product(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 10,
    name: 'Mechanical Keyboard',
    sku: 'SKU-1001',
    price: 129.9,
    stock: 25,
    status: 'ON_SALE',
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-01-01T10:00:00Z',
    ...overrides,
  }
}

export function order(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 5,
    userId: 1,
    userName: 'Alice Anderson',
    userEmail: 'alice@example.com',
    status: 'CREATED',
    totalAmount: 259.8,
    remark: 'Please deliver soon',
    items: [
      {
        id: 1,
        productId: 10,
        productName: 'Mechanical Keyboard',
        unitPrice: 129.9,
        quantity: 2,
        subtotal: 259.8,
      },
    ],
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-01-01T10:00:00Z',
    ...overrides,
  }
}
