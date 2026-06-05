import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withInMemoryScrolling, withViewTransitions } from '@angular/router';

import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { provideGraphqlClient } from './core/graphql/graphql-client.provider';
import { authSessionInterceptor } from './core/interceptors/auth-session.interceptor';
import { correlationIdInterceptor } from './core/interceptors/correlation-id.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([correlationIdInterceptor, authSessionInterceptor])),
    provideAppInitializer(() => inject(AuthService).bootstrapSession()),
    provideRouter(
      routes,
      withViewTransitions(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }),
    ),
    provideGraphqlClient(),
  ],
};
