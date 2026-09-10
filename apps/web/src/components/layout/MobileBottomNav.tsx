import { BarChart3, Bot, CreditCard, LayoutDashboard, ReceiptText, Settings, WalletCards } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '../../lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Home', icon: LayoutDashboard },
  { to: '/transactions', label: 'Cards', icon: CreditCard },
  { to: '/receipts', label: 'Receipts', icon: ReceiptText },
  { to: '/budgets', label: 'Budget', icon: WalletCards },
  { to: '/analytics', label: 'Insights', icon: BarChart3 },
  { to: '/assistant', label: 'AI', icon: Bot },
  { to: '/settings', label: 'Settings', icon: Settings },
]

export function MobileBottomNav() {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-border bg-card/90 p-2 backdrop-blur-xl lg:hidden" aria-label="Mobile bottom navigation">
      <div className="grid grid-cols-7 gap-1">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/dashboard'}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center justify-center gap-1 rounded-xl px-1 py-2 text-[0.62rem] font-medium transition-colors',
                isActive
                  ? 'text-primary'
                  : 'text-muted-foreground hover:text-foreground',
              )
            }
          >
            {({ isActive }) => (
              <>
                <span
                  className={cn(
                    'flex h-8 w-8 items-center justify-center rounded-lg',
                    isActive ? 'bg-primary/10 text-primary' : 'bg-transparent',
                  )}
                >
                  <Icon className="h-4 w-4" />
                </span>
                <span>{label}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  )
}

export default MobileBottomNav
