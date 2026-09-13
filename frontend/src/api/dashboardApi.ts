import { buildQuery, request } from './http'
import type { DashboardSummary } from '../types/dashboard'
import type { OrderSummary } from '../types/order'

export function getDashboardSummary(): Promise<DashboardSummary> {
  return request<DashboardSummary>('/api/dashboard/summary')
}

export function getRecentOrders(limit = 5): Promise<OrderSummary[]> {
  return request<OrderSummary[]>(`/api/dashboard/recent-orders${buildQuery({ limit })}`)
}
