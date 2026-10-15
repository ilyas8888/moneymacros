import { useQuery } from '@tanstack/react-query'

/** Response of GET /api/ping. Hand-written for now; from Day 6 API types come
 *  from the generated OpenAPI file (ADR 005, .claude/rules/frontend.md). */
export interface Ping {
  status: string
  time: string
}

async function fetchPing(): Promise<Ping> {
  const response = await fetch('/api/ping')
  if (!response.ok) {
    throw new Error(`API responded ${response.status}`)
  }
  return (await response.json()) as Ping
}

export function usePing() {
  return useQuery({ queryKey: ['ping'], queryFn: fetchPing })
}
