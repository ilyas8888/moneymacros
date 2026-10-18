import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, api, type ProblemDetails } from '../../api/client'
import type { components } from '../../api/schema'
import type { User } from './useMe'

type LoginRequest = components['schemas']['LoginRequest']

/** Log in with email and password, then seed the cached identity. */
export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: LoginRequest): Promise<User> => {
      const { data, error, response } = await api.POST('/api/auth/login', {
        body,
      })
      if (data) {
        return data
      }
      throw new ApiError(response.status, error as ProblemDetails | undefined)
    },
    onSuccess: (user) => {
      queryClient.setQueryData(['me'], user)
    },
  })
}
