import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { RuntimeConfigService } from '../runtime-config/runtime-config.service';

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const PUBLIC_GRAPHQL_OPERATIONS = new Set([
  'Login',
  'Logout',
  'CurrentSession',
  'FrontendReadiness',
  'AcceptUserInvite',
  'RequestPasswordReset',
  'ResetPassword',
]);

function isBackendRequest(url: string, backendBaseUrl: string, graphqlEndpoint: string): boolean {
  return url.startsWith(backendBaseUrl) || url.startsWith(graphqlEndpoint);
}

function isPublicAuthRequest(
  request: HttpRequest<unknown>,
  backendBaseUrl: string,
  graphqlEndpoint: string,
): boolean {
  if (request.url.startsWith(`${backendBaseUrl}/auth/`)) {
    return true;
  }

  if (!request.url.startsWith(graphqlEndpoint)) {
    return false;
  }

  const body = request.body as { operationName?: unknown; query?: unknown } | null;
  if (!body || typeof body !== 'object') {
    return false;
  }

  if (typeof body.operationName === 'string' && PUBLIC_GRAPHQL_OPERATIONS.has(body.operationName)) {
    return true;
  }

  if (typeof body.query !== 'string') {
    return false;
  }

  const query = body.query;
  return [...PUBLIC_GRAPHQL_OPERATIONS].some((operation) =>
    new RegExp(`\\b(query|mutation)\\s+${operation}\\b`).test(query),
  );
}

export const authSessionInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const runtimeConfig = inject(RuntimeConfigService).config();
  const csrfToken = authService.csrfToken();
  const backendBaseUrl = runtimeConfig.backendBaseUrl;
  const graphqlEndpoint = runtimeConfig.graphql.endpoint;
  let headers = request.headers;

  if (csrfToken && UNSAFE_METHODS.has(request.method.toUpperCase())) {
    headers = headers.set(csrfToken.headerName, csrfToken.token);
  }

  const sessionRequest = isBackendRequest(request.url, backendBaseUrl, graphqlEndpoint)
    ? request.clone({
        headers,
        withCredentials: true,
      })
    : request.clone({ headers });

  return next(sessionRequest).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        isBackendRequest(sessionRequest.url, backendBaseUrl, graphqlEndpoint) &&
        !isPublicAuthRequest(sessionRequest, backendBaseUrl, graphqlEndpoint) &&
        (error.status === 401 || error.status === 403)
      ) {
        authService.handleUnauthorized();
      }

      return throwError(() => error);
    }),
  );
};
