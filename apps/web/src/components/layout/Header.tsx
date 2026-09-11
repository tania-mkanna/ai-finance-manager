import { Bell, LogOut, MoonStar, SunMedium } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../providers/AuthProvider'
import { cn } from '../../lib/utils'
import { BrandMark } from './BrandMark'

interface HeaderProps {
  onMenuClick: () => void
}

export function Header({ }: HeaderProps) {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    if (typeof document === 'undefined') return 'light'
    const saved = document.documentElement.getAttribute('data-theme')
    if (saved === 'dark' || saved === 'light') return saved
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-20 border-b border-border bg-background/80 backdrop-blur-xl">
      <div className="flex h-20 items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-3">

          <div className="lg:hidden">
            <BrandMark compact />
          </div>
        </div>

        <div className="ml-auto flex items-center gap-2 sm:gap-3">
          <button
            type="button"
            aria-label="Toggle color theme"
            onClick={() => setTheme((current) => (current === 'dark' ? 'light' : 'dark'))}
            className="hidden h-10 w-10 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground transition-colors hover:text-foreground sm:flex"
          >
            {theme === 'dark' ? <SunMedium className="h-4 w-4" /> : <MoonStar className="h-4 w-4" />}
          </button>

          <button
            type="button"
            aria-label="Notifications"
            className="hidden h-10 w-10 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground transition-colors hover:text-foreground sm:flex"
          >
            <Bell className="h-4 w-4" />
          </button>

          <div className="flex items-center gap-3 rounded-2xl border border-border bg-card px-2 py-2 sm:px-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-sm font-semibold text-primary">
              {user?.fullName?.charAt(0)?.toUpperCase() ?? 'U'}
            </div>

            <div className="hidden text-left sm:block">
              <p className="text-sm font-semibold text-foreground">{user?.fullName ?? 'User'}</p>
              <p className="text-[0.68rem] text-muted-foreground">{user?.email ?? 'finance@example.com'}</p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className={cn(
              'inline-flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground',
              'sm:px-3.5',
            )}
          >
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">Log out</span>
          </button>
        </div>
      </div>
    </header>
  )
}

export default Header
