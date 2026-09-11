import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { AuthLayout } from '../layouts/AuthLayout'
import { AppLayout } from '../layouts/AppLayout'
import { DashboardPage } from '../pages/Dashboard'
import LoginPage from '../pages/auth/Login'
import RegisterPage from '../pages/auth/Register'
import { ProtectedRoute } from './ProtectedRoute'
import { useAuth } from '../providers/AuthProvider'

function AuthRedirector() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return <div>Checking session...</div>
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthRedirector />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/transactions" element={<div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">Transactions page placeholder</div>} />
        <Route path="/receipts" element={<div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">Receipts page placeholder</div>} />
        <Route path="/budgets" element={<div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">Budgets page placeholder</div>} />
        <Route path="/analytics" element={<div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">Analytics page placeholder</div>} />
        <Route path="/assistant" element={<div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">AI Assistant page placeholder</div>} />
        <Route path="/settings" element={<div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">Settings page placeholder</div>} />
      </Route>

      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default AppRoutes
