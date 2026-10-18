import { ApiError } from '../../api/client'

/**
 * The web chooses error text from the status code, never the API's English
 * message. This maps an error to what the form should show.
 */
export type AuthErrorView =
  | { type: 'credentials' } // 401
  | { type: 'emailTaken' } // 409
  | { type: 'fields'; fields: string[] } // 400, with the invalid field names
  | { type: 'generic' } // anything else

export function toAuthErrorView(error: unknown): AuthErrorView {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return { type: 'credentials' }
    }
    if (error.status === 409) {
      return { type: 'emailTaken' }
    }
    if (error.status === 400) {
      const fields = error.problem?.errors?.map((e) => e.field) ?? []
      if (fields.length > 0) {
        return { type: 'fields', fields }
      }
    }
  }
  return { type: 'generic' }
}
