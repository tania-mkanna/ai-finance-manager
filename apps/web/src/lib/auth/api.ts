import apiClient from '../api-client'
import type {
  AuthResponse,
  LoginRequest,
  RefreshResponse,
  RegisterRequest,
  UserResponse,
} from './types'

const AUTH_PATH = '/auth'

export const authApi = {
  login: (payload: LoginRequest) =>
    apiClient.post<AuthResponse>(`${AUTH_PATH}/login`, payload),

  register: (payload: RegisterRequest) =>
    apiClient.post<AuthResponse>(`${AUTH_PATH}/register`, payload),

  refresh: () => apiClient.post<RefreshResponse>(`${AUTH_PATH}/refresh`),

  logout: () => apiClient.post<void>(`${AUTH_PATH}/logout`),

  getCurrentUser: () => apiClient.get<UserResponse>(`${AUTH_PATH}/me`),
}

export default authApi
