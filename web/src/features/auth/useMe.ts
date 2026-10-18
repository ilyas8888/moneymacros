import { useQuery } from '@tanstack/react-query'
import { ApiError, api, type ProblemDetails } from '../../api/client'
import type { components } from '../../api/schema'

export type User = components['schemas']['UserResponse']

/**
 * The single source of truth for who is logged in. A 401 means "logged out"
 * (null), not an error, so the UI never shows an error for it and it is never
 * retried.
 */
async function fetchMe(): Promise<User | null> {
  const { data, error, response } = await api.GET('/api/me')
  // Read the status up front: the contract documents no error responses, so the
  // typed result narrows its failure branch away and `response` would otherwise
  // be seen as unreachable there.
  const status = response.status
  if (status === 401) {
    return null
  }
  if (error || !data) {
    throw new ApiError(status, error as ProblemDetails | undefined)
  }
  return data
}

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: fetchMe, retry: false })
}
