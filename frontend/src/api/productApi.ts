import { buildQuery, request } from './http'
import type { PageResponse } from '../types/common'
import type {
  CreateProductPayload,
  Product,
  ProductSearchParams,
  ProductStatus,
  UpdateProductPayload,
} from '../types/product'

export function searchProducts(params: ProductSearchParams = {}): Promise<PageResponse<Product>> {
  return request<PageResponse<Product>>(`/api/products${buildQuery({ ...params })}`)
}

export function getProduct(id: number): Promise<Product> {
  return request<Product>(`/api/products/${id}`)
}

export function createProduct(payload: CreateProductPayload): Promise<Product> {
  return request<Product>('/api/products', { method: 'POST', body: payload })
}

export function updateProduct(id: number, payload: UpdateProductPayload): Promise<Product> {
  return request<Product>(`/api/products/${id}`, { method: 'PUT', body: payload })
}

export function updateProductStock(id: number, stock: number): Promise<Product> {
  return request<Product>(`/api/products/${id}/stock`, { method: 'PATCH', body: { stock } })
}

export function updateProductStatus(id: number, status: ProductStatus): Promise<Product> {
  return request<Product>(`/api/products/${id}/status`, { method: 'PATCH', body: { status } })
}

export function deleteProduct(id: number): Promise<void> {
  return request<void>(`/api/products/${id}`, { method: 'DELETE' })
}
