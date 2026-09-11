import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { ApiError } from '../lib/api-client'
import authApi from '../lib/auth/api'
import type { LoginRequest, RegisterRequest, UserResponse } from '../lib/auth/types'

export type AuthContextValue = {
  user: UserResponse | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (payload: LoginRequest) => Promise<UserResponse>
  register: (payload: RegisterRequest) => Promise<UserResponse>
  logout: () => Promise<void>
  refreshSession: () => Promise<UserResponse | null>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const refreshSession = useCallback(async () => {
    try {
      await authApi.refresh()
      const refreshedUser = await authApi.getCurrentUser()
      setUser(refreshedUser)
      return refreshedUser
    } catch {
      setUser(null)
      return null
    }
  }, [])

  const loadCurrentUser = useCallback(async () => {
    try {
      const currentUser = await authApi.getCurrentUser()
      setUser(currentUser)
      return currentUser
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        return refreshSession()
      }

      setUser(null)
      return null
    }
  }, [refreshSession])

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await authApi.login(payload)
    setUser(response.user)
    return response.user
  }, [])

  const register = useCallback(async (payload: RegisterRequest) => {
    const response = await authApi.register(payload)
    setUser(response.user)
    return response.user
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  useEffect(() => {
    void (async () => {
      setIsLoading(true)
      await loadCurrentUser()
      setIsLoading(false)
    })()
  }, [loadCurrentUser])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading,
      login,
      register,
      logout,
      refreshSession,
    }),
    [isLoading, login, logout, refreshSession, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }

  return context
}

export default AuthProvider
