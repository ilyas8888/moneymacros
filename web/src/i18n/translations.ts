// English is the source of truth for the set of keys. French is typed as
// `typeof en`, so a key missing from (or added only to) French is a compile
// error and fails `npm run build` — both languages always have the same keys.
export const en = {
  common: {
    appName: 'MoneyMacros',
    loading: 'Loading…',
  },
  language: {
    fr: 'FR',
    en: 'EN',
    switchLabel: 'Language',
  },
  home: {
    apiChecking: 'API: checking…',
    apiError: 'API: error',
    apiStatus: 'API: {{status}} (v{{version}})',
    greeting: 'Signed in as {{email}}.',
    logout: 'Log out',
    logoutError: "Couldn't log out. Please try again.",
  },
  auth: {
    login: {
      title: 'Log in',
      submit: 'Log in',
      switchPrompt: 'No account yet?',
      switchLink: 'Create one',
    },
    signup: {
      title: 'Create your account',
      submit: 'Sign up',
      switchPrompt: 'Already have an account?',
      switchLink: 'Log in',
    },
    email: { label: 'Email' },
    password: { label: 'Password' },
    errors: {
      invalidCredentials: 'Invalid email or password.',
      emailTaken: 'This email already has an account.',
      emailTakenLink: 'Log in instead',
      generic: 'Something went wrong. Please try again.',
    },
    fieldErrors: {
      email: 'Enter a valid email address.',
      password: 'Password must be between {{min}} and {{max}} characters.',
    },
  },
}

export const fr: typeof en = {
  common: {
    appName: 'MoneyMacros',
    loading: 'Chargement…',
  },
  language: {
    fr: 'FR',
    en: 'EN',
    switchLabel: 'Langue',
  },
  home: {
    apiChecking: 'API : vérification…',
    apiError: 'API : erreur',
    apiStatus: 'API : {{status}} (v{{version}})',
    greeting: 'Connecté en tant que {{email}}.',
    logout: 'Se déconnecter',
    logoutError: 'La déconnexion a échoué. Veuillez réessayer.',
  },
  auth: {
    login: {
      title: 'Connexion',
      submit: 'Se connecter',
      switchPrompt: 'Pas encore de compte ?',
      switchLink: 'Créez-en un',
    },
    signup: {
      title: 'Créer votre compte',
      submit: "S'inscrire",
      switchPrompt: 'Vous avez déjà un compte ?',
      switchLink: 'Se connecter',
    },
    email: { label: 'E-mail' },
    password: { label: 'Mot de passe' },
    errors: {
      invalidCredentials: 'E-mail ou mot de passe invalide.',
      emailTaken: 'Un compte existe déjà pour cet e-mail.',
      emailTakenLink: 'Se connecter',
      generic: "Une erreur s'est produite. Veuillez réessayer.",
    },
    fieldErrors: {
      email: 'Saisissez une adresse e-mail valide.',
      password:
        'Le mot de passe doit comporter entre {{min}} et {{max}} caractères.',
    },
  },
}

export const resources = {
  en: { translation: en },
  fr: { translation: fr },
}
