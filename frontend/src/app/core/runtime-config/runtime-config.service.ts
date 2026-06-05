import { Injectable, signal } from '@angular/core';

import { environment } from '../../../environments/environment';
import { AuthStarterEnvironment } from '../../../environments/environment.model';

type RuntimeConfig = Partial<
  Pick<AuthStarterEnvironment, 'appVersion' | 'backendBaseUrl' | 'graphql' | 'i18n'>
>;

const DEFAULT_CONFIG = environment as AuthStarterEnvironment;

@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private readonly configState = signal<AuthStarterEnvironment>(DEFAULT_CONFIG);
  private loadPromise: Promise<void> | null = null;

  readonly config = this.configState.asReadonly();

  requiresRemoteConfig(): boolean {
    return Boolean(DEFAULT_CONFIG.runtimeConfigPath);
  }

  load(): Promise<void> {
    if (this.loadPromise) {
      return this.loadPromise;
    }

    if (!DEFAULT_CONFIG.runtimeConfigPath) {
      this.loadPromise = Promise.resolve();
      return this.loadPromise;
    }

    this.loadPromise = fetch(DEFAULT_CONFIG.runtimeConfigPath, {
      cache: 'no-store',
      credentials: 'same-origin',
    })
      .then((response) => {
        if (!response.ok) {
          return null;
        }
        return response.json() as Promise<RuntimeConfig>;
      })
      .then((runtimeConfig) => {
        if (!runtimeConfig) {
          return;
        }

        this.configState.set({
          ...DEFAULT_CONFIG,
          ...runtimeConfig,
          graphql: {
            ...DEFAULT_CONFIG.graphql,
            ...(runtimeConfig.graphql ?? {}),
          },
          i18n: {
            ...DEFAULT_CONFIG.i18n,
            ...(runtimeConfig.i18n ?? {}),
          },
        });
      })
      .catch(() => undefined);

    return this.loadPromise;
  }
}
