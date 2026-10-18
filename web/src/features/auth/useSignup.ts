import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiError, api, type ProblemDetails } from '../../api/client'
import type { components } from '../../api/schema'
import type { User } from './useMe'

type SignupRequest = components['schemas']['SignupRequest']

/** Create an account (the API logs the new user in), then seed the identity. */
export function useSignup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (body: SignupRequest): Promise<User> => {
      const { data, error, response } = await api.POST('/api/auth/signup', {
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
