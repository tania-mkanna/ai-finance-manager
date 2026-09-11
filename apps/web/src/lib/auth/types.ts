export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  defaultCurrency: string;
  timezone: string;
}

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  defaultCurrency: string;
  timezone: string;
}

export interface AuthResponse {
  user: UserResponse;
}

export interface RefreshResponse {
  message: string;
}