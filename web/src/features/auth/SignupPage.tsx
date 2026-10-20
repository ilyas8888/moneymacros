import { type FormEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
    <div className="flex min-h-svh items-center justify-center bg-background p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <div className="flex justify-end">
            <LanguageSwitcher />
          </div>
          <CardTitle className="text-2xl">{t('auth.signup.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">{t('auth.email.label')}</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
              {invalidFields?.has('email') && (
                <p role="alert" className="text-sm text-destructive">
                  {t('auth.fieldErrors.email')}
                </p>
              )}
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">{t('auth.password.label')}</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                required
                minLength={PASSWORD_MIN}
                maxLength={PASSWORD_MAX}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              {invalidFields?.has('password') && (
                <p role="alert" className="text-sm text-destructive">
                  {t('auth.fieldErrors.password', {
                    min: PASSWORD_MIN,
                    max: PASSWORD_MAX,
                  })}
                </p>
              )}
            </div>
            {view?.type === 'emailTaken' && (
              <p role="alert" className="text-sm text-destructive">
                {t('auth.errors.emailTaken')}{' '}
                <Link
                  to="/login"
                  className="font-medium underline underline-offset-4"
                >
                  {t('auth.errors.emailTakenLink')}
                </Link>
              </p>
            )}
            {view?.type === 'generic' && (
              <p role="alert" className="text-sm text-destructive">
                {t('auth.errors.generic')}
              </p>
            )}
            <Button
              type="submit"
              className="w-full"
              disabled={signup.isPending}
            >
              {t('auth.signup.submit')}
            </Button>
          </form>
        </CardContent>
        <CardFooter className="justify-center">
          <p className="text-sm text-muted-foreground">
            {t('auth.signup.switchPrompt')}{' '}
            <Link
              to="/login"
              className="font-medium text-primary underline-offset-4 hover:underline"
            >
              {t('auth.signup.switchLink')}
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  )
}
