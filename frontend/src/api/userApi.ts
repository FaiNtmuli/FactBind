import { request as factBindRequest } from '../factbind'
import type { PageResponse } from '../types/common'
import type {
  CreateUserPayload,
  UpdateUserPayload,
  User,
  UserSearchParams,
  UserStatus,
} from '../types/user'

export function searchUsers(params: UserSearchParams = {}): Promise<PageResponse<User>> {
  return factBindRequest<PageResponse<User>>('User.List', { params: { ...params } })
}

export function getUser(id: number): Promise<User> {
  return factBindRequest<User>('User.Get', { params: { id } })
}

export function createUser(payload: CreateUserPayload): Promise<User> {
  return factBindRequest<User>('User.Create', { body: payload })
}

export function updateUser(id: number, payload: UpdateUserPayload): Promise<User> {
  return factBindRequest<User>('User.Update', { params: { id }, body: payload })
}

export function updateUserStatus(id: number, status: UserStatus): Promise<User> {
  return factBindRequest<User>('User.UpdateStatus', { params: { id }, body: { status } })
}

export function deleteUser(id: number): Promise<void> {
  return factBindRequest<void>('User.Delete', { params: { id } })
}
