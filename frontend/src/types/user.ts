export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface User {
  id: number
  name: string
  email: string
  age: number
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface CreateUserPayload {
  name: string
  email: string
  age: number
  status?: UserStatus
}

export type UpdateUserPayload = Omit<CreateUserPayload, 'status'>

export interface UserSearchParams {
  keyword?: string
  status?: UserStatus
  page?: number
  size?: number
}
