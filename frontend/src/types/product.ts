export type ProductStatus = 'ON_SALE' | 'OFF_SALE'

export interface Product {
  id: number
  name: string
  sku: string
  price: number
  stock: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

export interface CreateProductPayload {
  name: string
  sku: string
  price: number
  stock: number
  status?: ProductStatus
}

export type UpdateProductPayload = Omit<CreateProductPayload, 'status'>

export interface ProductSearchParams {
  keyword?: string
  status?: ProductStatus
  page?: number
  size?: number
}
