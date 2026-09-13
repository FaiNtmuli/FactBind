import { buildQuery, request } from './http'
import type { PageResponse } from '../types/common'
import type {
  CreateOrderPayload,
  Order,
  OrderSearchParams,
  OrderStatus,
} from '../types/order'

export function searchOrders(params: OrderSearchParams = {}): Promise<PageResponse<Order>> {
  return request<PageResponse<Order>>(`/api/orders${buildQuery({ ...params })}`)
}

export function getOrder(id: number): Promise<Order> {
  return request<Order>(`/api/orders/${id}`)
}

export function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return request<Order>('/api/orders', { method: 'POST', body: payload })
}

export function updateOrderStatus(id: number, status: OrderStatus, notify = false): Promise<Order> {
  return request<Order>(`/api/orders/${id}/status${buildQuery({ notify })}`, {
    method: 'PATCH',
    body: { status },
  })
}

export function deleteOrder(id: number): Promise<void> {
  return request<void>(`/api/orders/${id}`, { method: 'DELETE' })
}
