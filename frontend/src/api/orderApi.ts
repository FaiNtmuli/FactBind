import { request as factBindRequest } from '../factbind'
import type { PageResponse } from '../types/common'
import type {
  CreateOrderPayload,
  Order,
  OrderSearchParams,
  OrderStatus,
} from '../types/order'

export function searchOrders(params: OrderSearchParams = {}): Promise<PageResponse<Order>> {
  return factBindRequest<PageResponse<Order>>('Order.List', { params: { ...params } })
}

export function getOrder(id: number): Promise<Order> {
  return factBindRequest<Order>('Order.Get', { params: { id } })
}

export function createOrder(payload: CreateOrderPayload): Promise<Order> {
  return factBindRequest<Order>('Order.Create', { body: payload })
}

export function updateOrderStatus(id: number, status: OrderStatus, notify = false): Promise<Order> {
  return factBindRequest<Order>('Order.UpdateStatus', { params: { id, notify }, body: { status } })
}

export function deleteOrder(id: number): Promise<void> {
  return factBindRequest<void>('Order.Delete', { params: { id } })
}
