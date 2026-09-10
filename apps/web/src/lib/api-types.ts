export interface ApiErrorBody {
  message?: string
  error?: string
  details?: string[]
}

export interface PaginationMeta {
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

export interface PaginatedResponse<T> {
  items: T[]
  meta: PaginationMeta
}

export interface ApiListResponse<T> {
  items: T[]
  total?: number
}

export interface ApiResponse<T> {
  data: T
}
