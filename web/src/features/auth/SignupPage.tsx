import { type FormEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { LanguageSwitcher } from '../../i18n/LanguageSwitcher'
import { toAuthErrorView } from './authError'
import { PASSWORD_MAX, PASSWORD_MIN } from './passwordRules'
import { useSignup } from './useSignup'

export function SignupPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const signup = useSignup()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const view = signup.isError ? toAuthErrorView(signup.error) : undefined
  const invalidFields =
    view?.type === 'fields' ? new Set(view.fields) : undefined

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    signup.mutate(
      { email, password },
      { onSuccess: () => navigate('/', { replace: true }) },
    )
  }

  return (
    <main>
      <LanguageSwitcher />
      <h1>{t('auth.signup.title')}</h1>
      <form onSubmit={onSubmit} noValidate>
        <label>
          {t('auth.email.label')}
          <input
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        {invalidFields?.has('email') && (
          <p role="alert">{t('auth.fieldErrors.email')}</p>
        )}
        <label>
          {t('auth.password.label')}
          <input
            type="password"
            autoComplete="new-password"
            required
            minLength={PASSWORD_MIN}
            maxLength={PASSWORD_MAX}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        {invalidFields?.has('password') && (
          <p role="alert">{t('auth.fieldErrors.password')}</p>
        )}
        {view?.type === 'emailTaken' && (
          <p role="alert">
            {t('auth.errors.emailTaken')}{' '}
            <Link to="/login">{t('auth.errors.emailTakenLink')}</Link>
          </p>
        )}
        {view?.type === 'generic' && (
          <p role="alert">{t('auth.errors.generic')}</p>
        )}
        <button type="submit" disabled={signup.isPending}>
          {t('auth.signup.submit')}
        </button>
      </form>
      <p>
        {t('auth.signup.switchPrompt')}{' '}
        <Link to="/login">{t('auth.signup.switchLink')}</Link>
      </p>
    </main>
  )
}
