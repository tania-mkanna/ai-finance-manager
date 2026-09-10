import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/button'
import { ApiError } from '../../lib/api-client'
import { useAuth } from '../../providers/AuthProvider'

const initialState = {
  email: '',
  password: '',
  fullName: '',
  defaultCurrency: 'USD',
  timezone: 'UTC',
}

export function RegisterPage() {
  const navigate = useNavigate()
  const { register, isAuthenticated, isLoading } = useAuth()
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
    const fullName = form.fullName.trim()
    const defaultCurrency = form.defaultCurrency.trim().toUpperCase()
    const timezone = form.timezone.trim()

    if (!email || !password || !fullName || !defaultCurrency || !timezone) {
      setError('Please complete all fields.')
      return
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailPattern.test(email)) {
      setError('Enter a valid email address.')
      return
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long.')
      return
    }

    if (!/[A-Za-z]/.test(password) || !/\d/.test(password) || !/[@#$%^&+=!_.*?]/.test(password)) {
      setError('Password must contain letters, numbers, and special characters.')
      return
    }

    if (defaultCurrency.length !== 3) {
      setError('Currency must be a 3-letter ISO code.')
      return
    }

    setIsSubmitting(true)
    setError('')

    try {
      await register({
        email,
        password,
        fullName,
        defaultCurrency,
        timezone,
      })

      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      const message =
        requestError instanceof ApiError
          ? requestError.message
          : 'Unable to create your account. Please try again.'
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="w-full">
      <h2 className="mb-2 text-3xl font-semibold tracking-tight text-foreground">Create account</h2>
      <p className="mb-6 text-sm text-muted-foreground">Set up your personal finance account.</p>

      {error ? (
        <div role="alert" className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200">
          {error}
        </div>
      ) : null}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <label htmlFor="register-name" className="text-sm font-medium text-foreground">
            Full name
          </label>
          <input
            id="register-name"
            value={form.fullName}
            onChange={(event) => handleChange('fullName', event.target.value)}
            placeholder="Jane Doe"
            className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="register-email" className="text-sm font-medium text-foreground">
            Email address
          </label>
          <input
            id="register-email"
            type="email"
            value={form.email}
            onChange={(event) => handleChange('email', event.target.value)}
            placeholder="name@example.com"
            className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="register-password" className="text-sm font-medium text-foreground">
            Password
          </label>
          <input
            id="register-password"
            type="password"
            value={form.password}
            onChange={(event) => handleChange('password', event.target.value)}
            placeholder="Create a secure password"
            className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
          />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <label htmlFor="register-currency" className="text-sm font-medium text-foreground">
              Currency
            </label>
            <input
              id="register-currency"
              value={form.defaultCurrency}
              onChange={(event) => handleChange('defaultCurrency', event.target.value)}
              placeholder="USD"
              maxLength={3}
              className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
            />
          </div>

          <div className="space-y-2">
            <label htmlFor="register-timezone" className="text-sm font-medium text-foreground">
              Timezone
            </label>
            <input
              id="register-timezone"
              value={form.timezone}
              onChange={(event) => handleChange('timezone', event.target.value)}
              placeholder="UTC"
              className="w-full rounded-xl border border-input bg-background px-4 py-3 text-base text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/20"
            />
          </div>
        </div>

        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? 'Creating account...' : 'Create account'}
        </Button>
      </form>

      <p className="mt-5 text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-primary transition-colors hover:text-violet-500">
          Sign in
        </Link>
      </p>
    </div>
  )
}

export default RegisterPage
