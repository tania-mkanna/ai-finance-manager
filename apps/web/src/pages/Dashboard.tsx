import { Navigate, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { useAuth } from '../providers/AuthProvider'

export function DashboardPage() {
  const navigate = useNavigate()
  const { user, isAuthenticated, isLoading, logout } = useAuth()

  if (isLoading) {
    return <div className="grid min-h-screen place-items-center bg-background text-sm text-muted-foreground">Loading dashboard...</div>
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />
  }

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="grid min-h-screen place-items-center bg-background p-6">
      <div className="w-full max-w-xl rounded-[1.75rem] border border-border bg-card p-6 shadow-[0_25px_60px_rgba(15,23,42,0.08)] sm:p-8">
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-primary">Dashboard</p>
        <h1 className="mt-3 text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
          Welcome, {user.fullName}
        </h1>
        <p className="mt-3 text-base leading-7 text-muted-foreground">
          You are signed in and ready to continue building your financial dashboard.
        </p>

        <Button onClick={handleLogout} variant="outline" className="mt-6 w-full">
          Logout
        </Button>
      </div>
    </div>
  )
}

export default DashboardPage
