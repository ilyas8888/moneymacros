import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { components } from '../../api/schema'

/** Response of GET /api/ping, from the generated OpenAPI types (ADR 005). */
export type Ping = components['schemas']['PingResponse']

async function fetchPing(): Promise<Ping> {
  const { data, error } = await api.GET('/api/ping')
  if (error || !data) {
    throw new Error('The API did not return a valid ping response.')
  }
  return data
}

export function usePing() {
  return useQuery({ queryKey: ['ping'], queryFn: fetchPing })
}
