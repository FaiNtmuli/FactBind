export type OrderStatus = 'CREATED' | 'PAID' | 'CANCELLED' | 'COMPLETED'

export interface OrderItem {
  id: number
  productId: number
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  userId: number
  userName: string
  userEmail: string
  status: OrderStatus
  totalAmount: number
  remark?: string
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export interface OrderSummary {
  id: number
  userId: number
  userName: string
  status: OrderStatus
  totalAmount: number
  itemCount: number
  createdAt: string
}

export interface CreateOrderItemPayload {
  productId: number
  quantity: number
}

export interface CreateOrderPayload {
  userId: number
  remark?: string
  items: CreateOrderItemPayload[]
}

export interface OrderSearchParams {
  userId?: number
  status?: OrderStatus
  page?: number
  size?: number
}
