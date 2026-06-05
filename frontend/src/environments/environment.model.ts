export type EnvironmentName = 'development' | 'production';
export type EnvironmentLocale = 'en' | 'fr' | 'zh';

export interface AuthStarterEnvironment {
  name: EnvironmentName;
  production: boolean;
  appVersion: string;
  runtimeConfigPath?: string;
  backendBaseUrl: string;
  graphql: {
    endpoint: string;
  };
  i18n: {
    defaultLocale: EnvironmentLocale;
    supportedLocales: readonly EnvironmentLocale[];
  };
}
