import { request as factBindRequest } from '../factbind'
import type { DashboardSummary } from '../types/dashboard'
import type { OrderSummary } from '../types/order'

export function getDashboardSummary(): Promise<DashboardSummary> {
  return factBindRequest<DashboardSummary>('Dashboard.Summary')
}

export function getRecentOrders(limit?: number): Promise<OrderSummary[]> {
  return factBindRequest<OrderSummary[]>('Dashboard.RecentOrders', { params: { limit } })
}
