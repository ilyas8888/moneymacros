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
  const { ok, status } = response
  // 401 means logged out (null), not an error. Decide from the HTTP status,
  // never from whether an error body was parsed (see useLogout).
  if (status === 401) {
    return null
  }
  if (!ok || !data) {
    throw new ApiError(status, error as ProblemDetails | undefined)
  }
  return data
}

export function useMe() {
  return useQuery({ queryKey: ['me'], queryFn: fetchMe, retry: false })
}
