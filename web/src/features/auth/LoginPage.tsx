import { type FormEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, type Location, useLocation, useNavigate } from 'react-router-dom'
import { LanguageSwitcher } from '../../i18n/LanguageSwitcher'
import { toAuthErrorView } from './authError'
import { PASSWORD_MAX, PASSWORD_MIN } from './passwordRules'
import { useLogin } from './useLogin'

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const login = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const from = (location.state as { from?: Location } | null)?.from ?? '/'
  const view = login.isError ? toAuthErrorView(login.error) : undefined
  const invalidFields =
    view?.type === 'fields' ? new Set(view.fields) : undefined

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    login.mutate(
      { email, password },
      { onSuccess: () => navigate(from, { replace: true }) },
    )
  }

  return (
    <main>
      <LanguageSwitcher />
      <h1>{t('auth.login.title')}</h1>
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
            autoComplete="current-password"
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
        {view && view.type !== 'fields' && (
          <p role="alert">
            {view.type === 'credentials'
              ? t('auth.errors.invalidCredentials')
              : t('auth.errors.generic')}
          </p>
        )}
        <button type="submit" disabled={login.isPending}>
          {t('auth.login.submit')}
        </button>
      </form>
      <p>
        {t('auth.login.switchPrompt')}{' '}
        <Link to="/signup">{t('auth.login.switchLink')}</Link>
      </p>
    </main>
  )
}
