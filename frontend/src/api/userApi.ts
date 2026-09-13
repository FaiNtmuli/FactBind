import { buildQuery, request } from './http'
import type { PageResponse } from '../types/common'
import type {
  CreateUserPayload,
  UpdateUserPayload,
  User,
  UserSearchParams,
  UserStatus,
} from '../types/user'

export function searchUsers(params: UserSearchParams = {}): Promise<PageResponse<User>> {
  return request<PageResponse<User>>(`/api/users${buildQuery({ ...params })}`)
}

export function getUser(id: number): Promise<User> {
  return request<User>(`/api/users/${id}`)
}

export function createUser(payload: CreateUserPayload): Promise<User> {
  return request<User>('/api/users', { method: 'POST', body: payload })
}

export function updateUser(id: number, payload: UpdateUserPayload): Promise<User> {
  return request<User>(`/api/users/${id}`, { method: 'PUT', body: payload })
}

export function updateUserStatus(id: number, status: UserStatus): Promise<User> {
  return request<User>(`/api/users/${id}/status`, { method: 'PATCH', body: { status } })
}

export function deleteUser(id: number): Promise<void> {
  return request<void>(`/api/users/${id}`, { method: 'DELETE' })
}
