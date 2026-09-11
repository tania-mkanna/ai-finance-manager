import { Sparkles } from 'lucide-react'
import { cn } from '../../lib/utils'

interface BrandMarkProps {
  className?: string
  compact?: boolean
}

export function BrandMark({ className, compact = false }: BrandMarkProps) {
  return (
    <div className={cn('flex items-center gap-3', className)}>
      <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-primary via-violet-500 to-accent text-primary-foreground shadow-[0_12px_30px_rgba(124,58,237,0.28)] ring-1 ring-white/10">
        <Sparkles className="h-5 w-5" />
      </div>

      {!compact ? (
        <div className="flex min-w-0 flex-col">
          <span className="text-[0.6rem] font-semibold uppercase tracking-[0.18em] text-violet-500">
            AI Finance
          </span>
          <span className="text-base font-semibold text-foreground">Manager</span>
        </div>
      ) : null}
    </div>
  )
}

export default BrandMark
