export function AuthHeader() {
  return (
    <header className="mb-5 flex items-center gap-4">
      <div className="flex items-center gap-3">
        <div className="grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br from-primary to-violet-500 text-lg font-bold text-primary-foreground shadow-[0_16px_35px_rgba(139,92,246,0.35)]">
          FM
        </div>

        <div>
          <div className="text-[0.68rem] font-bold uppercase tracking-[0.18em] text-violet-600 dark:text-violet-300">
            Finance Manager
          </div>
        </div>
      </div>
    </header>
  )
}
