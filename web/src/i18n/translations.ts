// English is the source of truth for the set of keys. French is typed as
// `typeof en`, so a key missing from (or added only to) French is a compile
// error and fails `npm run build` — both languages always have the same keys.
export const en = {
  common: {
    appName: 'MoneyMacros',
  },
  language: {
    fr: 'FR',
    en: 'EN',
    switchLabel: 'Language',
  },
  home: {
    apiChecking: 'API: checking…',
    apiError: 'API: error ({{message}})',
    apiStatus: 'API: {{status}} (v{{version}})',
  },
}

export const fr: typeof en = {
  common: {
    appName: 'MoneyMacros',
  },
  language: {
    fr: 'FR',
    en: 'EN',
    switchLabel: 'Langue',
  },
  home: {
    apiChecking: 'API : vérification…',
    apiError: 'API : erreur ({{message}})',
    apiStatus: 'API : {{status}} (v{{version}})',
  },
}

export const resources = {
  en: { translation: en },
  fr: { translation: fr },
}
