import { BarChart3, Bot, BriefcaseBusiness, CreditCard, LayoutDashboard, ReceiptText, Settings, WalletCards } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '../../lib/utils'
import { BrandMark } from './BrandMark'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/transactions', label: 'Transactions', icon: CreditCard },
  { to: '/receipts', label: 'Receipts', icon: ReceiptText },
  { to: '/budgets', label: 'Budgets', icon: WalletCards },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/assistant', label: 'AI Assistant', icon: Bot },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export function Sidebar() {
  return (
    <aside className="hidden w-72 shrink-0 border-r border-border bg-card/80 backdrop-blur-xl lg:flex lg:flex-col">
      <div className="flex h-20 items-center border-b border-border px-6">
        <BrandMark />
      </div>

      <nav className="flex-1 space-y-2 px-4 py-5" aria-label="Sidebar navigation">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/dashboard'}
            className={({ isActive }) =>
              cn(
                'group flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-medium transition-all',
                isActive
                  ? 'bg-primary/10 text-primary shadow-sm ring-1 ring-primary/20'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
              )
            }
          >
            {({ isActive }) => (
              <>
                <span
                  className={cn(
                    'flex h-9 w-9 items-center justify-center rounded-xl border transition-colors',
                    isActive
                      ? 'border-primary/20 bg-primary text-primary-foreground'
                      : 'border-border bg-background text-muted-foreground group-hover:text-foreground',
                  )}
                >
                  <Icon className="h-4 w-4" />
                </span>
                <span>{label}</span>
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-border p-4">
        <div className="flex items-center gap-3 rounded-2xl border border-border bg-secondary/60 p-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <BriefcaseBusiness className="h-4 w-4" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-foreground">Goals</p>
            <p className="text-xs text-muted-foreground">Emergency fund</p>
          </div>
        </div>
      </div>
    </aside>
  )
}

export default Sidebar
