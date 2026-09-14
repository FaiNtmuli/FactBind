import { request as factBindRequest } from '../factbind'
import type { PageResponse } from '../types/common'
import type {
  CreateProductPayload,
  Product,
  ProductSearchParams,
  ProductStatus,
  UpdateProductPayload,
} from '../types/product'

export function searchProducts(params: ProductSearchParams = {}): Promise<PageResponse<Product>> {
  return factBindRequest<PageResponse<Product>>('Product.List', { params: { ...params } })
}

export function getProduct(id: number): Promise<Product> {
  return factBindRequest<Product>('Product.Get', { params: { id } })
}

export function createProduct(payload: CreateProductPayload): Promise<Product> {
  return factBindRequest<Product>('Product.Create', { body: payload })
}

export function updateProduct(id: number, payload: UpdateProductPayload): Promise<Product> {
  return factBindRequest<Product>('Product.Update', { params: { id }, body: payload })
}

export function updateProductStock(id: number, stock: number): Promise<Product> {
  return factBindRequest<Product>('Product.UpdateStock', { params: { id }, body: { stock } })
}

export function updateProductStatus(id: number, status: ProductStatus): Promise<Product> {
  return factBindRequest<Product>('Product.UpdateStatus', { params: { id }, body: { status } })
}

export function deleteProduct(id: number): Promise<void> {
  return factBindRequest<void>('Product.Delete', { params: { id } })
}
