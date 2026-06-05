import type { AuthStarterEnvironment } from './environment.model';

export const environment = {
  name: 'development',
  production: false,
  appVersion: '0.0.0',
  backendBaseUrl: 'http://localhost:8080',
  graphql: {
    endpoint: 'http://localhost:8080/graphql',
  },
  i18n: {
    defaultLocale: 'en',
    supportedLocales: ['en', 'fr', 'zh'],
  },
} satisfies AuthStarterEnvironment;
