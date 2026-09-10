import { NavLink } from 'react-router-dom'

const tabs = [
  { label: 'Sign in', to: '/login' },
  { label: 'Create account', to: '/register' },
]

export function AuthTabs() {
  return (
    <nav className="mb-6 flex gap-2 rounded-xl bg-muted p-1.5">
      {tabs.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/login'}
          className={({ isActive }) =>
            [
              'flex-1 rounded-lg px-4 py-2.5 text-center text-sm font-semibold transition-colors',
              isActive
                ? 'bg-background text-foreground shadow-sm ring-1 ring-border'
                : 'text-muted-foreground hover:text-foreground',
            ].join(' ')
          }
        >
          {tab.label}
        </NavLink>
      ))}
    </nav>
  )
}
