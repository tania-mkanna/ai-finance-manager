import { Outlet } from 'react-router-dom'
import { AuthHeader } from '../components/auth/AuthHeader'
import { AuthHero } from '../components/auth/AuthHero'
import { AuthTabs } from '../components/auth/AuthTabs'
import { useAuthLayoutAnimation } from '../hooks/useAuthLayoutAnimation'

export function AuthLayout() {
  const containerRef = useAuthLayoutAnimation()

  return (
    <div ref={containerRef} className="grid min-h-screen place-items-center bg-background p-4 sm:p-6 lg:p-8">
      <div className="w-full max-w-6xl overflow-hidden rounded-[2rem] border border-border/80 bg-card/80 shadow-[0_30px_80px_rgba(15,23,42,0.08)] backdrop-blur-sm">
        <div className="px-5 pb-4 pt-6 sm:px-8 lg:px-10 lg:pb-6">
          <AuthHeader />
        </div>

        <div className="grid gap-6 px-5 pb-5 sm:px-8 lg:grid-cols-[1.1fr_0.9fr] lg:gap-8 lg:px-10 lg:pb-8">
          <AuthHero />

          <div className="flex flex-col justify-center">
            <AuthTabs />
            <Outlet />
          </div>
        </div>
      </div>
    </div>
  )
}

export default AuthLayout
