import { useEffect, useRef } from 'react'

export function useAuthLayoutAnimation() {
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!containerRef.current) {
      return
    }

    const element = containerRef.current
    const frame = window.requestAnimationFrame(() => {
      element.classList.add('is-ready')
    })

    return () => window.cancelAnimationFrame(frame)
  }, [])

  return containerRef
}
