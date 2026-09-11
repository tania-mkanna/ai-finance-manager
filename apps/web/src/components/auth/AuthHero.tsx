export function AuthHero() {


  return (
    <aside className="relative flex min-h-[28rem] flex-col justify-between overflow-hidden rounded-[1.75rem] bg-gradient-to-br from-slate-950 via-violet-950 to-indigo-900 p-6 text-slate-200 shadow-[0_30px_60px_rgba(79,70,229,0.30)] sm:p-8">
      <div className="relative z-10">
        <span className="inline-flex items-center rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-[0.65rem] font-semibold uppercase tracking-[0.18em] text-violet-200">
          Welcome back
        </span>

        <h1 className="mt-5 max-w-lg text-4xl font-semibold leading-tight text-white sm:text-5xl">
          Manage your money with confidence.
        </h1>

        <p className="mt-4 max-w-xl text-base leading-7 text-slate-300">
          Keep track of spending, budgets, and financial goals in one secure place built for everyday clarity.
        </p>
      </div>
    
    </aside>
  )
}
