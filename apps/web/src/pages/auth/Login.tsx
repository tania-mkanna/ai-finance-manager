import { useState } from 'react'
import { Link, Navigate, useNavigate, useLocation } from 'react-router-dom'
import { Button } from '../../components/ui/button'
import { ApiError } from '../../lib/api-client'
import { useAuth } from '../../providers/AuthProvider'

const initialState = {
  email: '',
  password: '',
}

export function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate()
  const { login, isAuthenticated, isLoading } = useAuth()
  const [form, setForm] = useState(initialState)
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isLoading) {
    return <div className="py-8 text-sm text-muted-foreground">Checking session...</div>
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  const handleChange = (field: keyof typeof form, value: string) => {
    setForm((previous) => ({ ...previous, [field]: value }))
    if (error) {
      setError('')
    }
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const email = form.email.trim()
    const password = form.password

    if (!email || !password) {
      setError('Email and password are required.')
      return
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailPattern.test(email)) {
      setError('Enter a valid email address.')
      return
    }

    setIsSubmitting(true)
    setError('')

    try {
      await login({ email, password })

      const from = location.state?.from

      const destination =
        from?.pathname && from.pathname.startsWith('/')
          ? `${from.pathname}${from.search ?? ''}${from.hash ?? ''}`
          : '/dashboard'

      navigate(destination, { replace: true })
    } catch (requestError) {
      const message =
        requestError instanceof ApiError
          ? requestError.message
          : 'Unable to sign in. Please try again.'
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="w-full">
      <h2 className="mb-2 text-3xl font-semibold tracking-tight text-foreground">Welcome back</h2>
      <p className="mb-6 text-sm text-muted-foreground">Sign in to continue managing your finances.</p>

      {error ? (
        <div role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200">
          {error}
        </div>
      ) : null}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <label htmlFor="login-email" className="text-sm font-medium text-foreground">
            Email address
          </label>
          <input
            id="login-email"
            type="email"
            value={form.email}
            onChange={(event) => handleChange('email', event.target.value)}
            placeholder="name@example.com"
            className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="login-password" className="text-sm font-medium text-foreground">
            Password
          </label>
          <input
            id="login-password"
            type="password"
            value={form.password}
            onChange={(event) => handleChange('password', event.target.value)}
            placeholder="Enter your password"
            className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
          />
        </div>

        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? 'Signing in...' : 'Sign in'}
        </Button>
      </form>

      <p className="mt-5 text-center text-sm text-muted-foreground">
        Need an account?{' '}
        <Link to="/register" className="font-semibold text-primary transition-colors hover:text-violet-500">
          Create one
        </Link>
      </p>
    </div>
  )
}

export default LoginPage
