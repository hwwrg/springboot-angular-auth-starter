import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { authSessionInterceptor } from '../interceptors/auth-session.interceptor';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authSessionInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('bootstraps the current backend session with a CSRF token', async () => {
    const bootstrap = service.bootstrapSession();

    const csrfRequest = httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`);
    expect(csrfRequest.request.withCredentials).toBeTrue();
    csrfRequest.flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const sessionRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(sessionRequest.request.withCredentials).toBeTrue();
    expect(sessionRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    sessionRequest.flush({
      data: {
        currentSession: {
          authenticated: true,
          mustChangePassword: false,
          principal: {
            id: 'baseline-operator',
            email: 'operator@authstarter.local',
            displayName: 'Baseline Operator',
            roles: ['SUPERADMIN'],
            mustChangePassword: false,
          },
        },
      },
    });

    const profileRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(profileRequest.request.body.query).toContain('query CurrentUserProfile');
    profileRequest.flush(currentUserProfileResponse());

    await bootstrap;

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.session().principal?.email).toBe('operator@authstarter.local');
    expect(service.currentOrganization()?.organizationDisplayName).toBe('Auth Starter Local');
    expect(service.hasAnyRole(['SUPERADMIN'])).toBeTrue();
    expect(service.hasAnyRole(['USER'])).toBeFalse();
  });

  it('bootstraps anonymous state when the backend has no session', async () => {
    const bootstrap = service.bootstrapSession();

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    httpTesting.expectOne(environment.graphql.endpoint).flush({
      data: {
        currentSession: {
          authenticated: false,
          mustChangePassword: false,
          principal: null,
        },
      },
    });

    await bootstrap;

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.session().principal).toBeNull();
  });

  it('does not load the current user profile while bootstrapping a forced password-change session', async () => {
    const bootstrap = service.bootstrapSession();

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    httpTesting.expectOne(environment.graphql.endpoint).flush({
      data: {
        currentSession: {
          authenticated: true,
          mustChangePassword: true,
          principal: {
            id: '30000000-0000-4000-8000-000000000099',
            email: 'recovery@example.test',
            displayName: 'Recovery Operator',
            roles: ['SUPERADMIN'],
            mustChangePassword: true,
          },
        },
      },
    });

    await bootstrap;

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.mustChangePassword()).toBeTrue();
    expect(service.currentOrganization()).toBeNull();
    httpTesting.expectNone((request) => request.body?.query?.includes('query CurrentUserProfile'));
  });

  it('logs in through the backend GraphQL auth mutation', async () => {
    const login = firstValueFrom(
      service.login({ email: 'operator@authstarter.local', password: 'authstarter-local-password' }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const loginRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(loginRequest.request.body.query).toContain('mutation Login');
    expect(loginRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    loginRequest.flush({
      data: {
        login: {
          authenticated: true,
          mustChangePassword: false,
          principal: {
            id: 'baseline-operator',
            email: 'operator@authstarter.local',
            displayName: 'Baseline Operator',
            roles: ['ORG_ADMIN'],
            mustChangePassword: false,
          },
        },
      },
    });

    httpTesting.expectOne(environment.graphql.endpoint).flush(currentUserProfileResponse('ORG_ADMIN'));

    const session = await login;

    expect(session.authenticated).toBeTrue();
    expect(service.sessionLabel()).toBe('Baseline Operator · Auth Starter Local');
    expect(service.hasAnyRole(['ORG_ADMIN'])).toBeTrue();
  });

  it('does not treat a failed login response as an expired session redirect', async () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    const login = firstValueFrom(
      service.login({ email: 'operator@authstarter.local', password: 'wrong-password' }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const loginRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(loginRequest.request.body.query).toContain('mutation Login');
    loginRequest.flush(null, { status: 401, statusText: 'Unauthorized' });

    await expectAsync(login).toBeRejected();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('logs out through the backend GraphQL auth mutation', async () => {
    const logout = firstValueFrom(service.logout());

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const logoutRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(logoutRequest.request.body.query).toContain('mutation Logout');
    expect(logoutRequest.request.headers.get('X-XSRF-TOKEN')).toBe('csrf-token');
    logoutRequest.flush({
      data: {
        logout: {
          authenticated: false,
          mustChangePassword: false,
          principal: null,
        },
      },
    });

    await logout;

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.session().principal).toBeNull();
  });

  it('changes the forced password and loads the current user profile after success', async () => {
    const changePassword = firstValueFrom(
      service.changeOwnPassword({ newPassword: 'new-db-backed-password' }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const changeRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(changeRequest.request.body.query).toContain('mutation ChangeOwnPassword');
    expect(changeRequest.request.body.variables.input).toEqual({
      newPassword: 'new-db-backed-password',
    });
    changeRequest.flush({
      data: {
        changeOwnPassword: {
          authenticated: true,
          mustChangePassword: false,
          principal: {
            id: '30000000-0000-4000-8000-000000000099',
            email: 'recovery@example.test',
            displayName: 'Recovery Operator',
            roles: ['SUPERADMIN'],
            mustChangePassword: false,
          },
        },
      },
    });

    const profileRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(profileRequest.request.body.query).toContain('query CurrentUserProfile');
    profileRequest.flush(currentUserProfileResponse());

    const session = await changePassword;

    expect(session.mustChangePassword).toBeFalse();
    expect(service.mustChangePassword()).toBeFalse();
    expect(service.currentOrganization()?.organizationDisplayName).toBe('Auth Starter Local');
  });

  it('accepts an invited user password setup through the backend GraphQL mutation', async () => {
    const acceptInvite = firstValueFrom(
      service.acceptUserInvite({
        token: 'opaque-invitation-token',
        newPassword: 'new-db-backed-password',
      }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const acceptRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(acceptRequest.request.body.query).toContain('mutation AcceptUserInvite');
    expect(acceptRequest.request.body.variables.input).toEqual({
      token: 'opaque-invitation-token',
      newPassword: 'new-db-backed-password',
    });
    acceptRequest.flush({
      data: {
        acceptUserInvite: {
          email: 'invited-user@example.test',
          status: 'ACTIVE',
        },
      },
    });

    const result = await acceptInvite;

    expect(result.status).toBe('ACTIVE');
  });

  it('requests a password reset through the backend GraphQL mutation', async () => {
    const requestReset = firstValueFrom(
      service.requestPasswordReset({ email: 'operator@authstarter.local' }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const resetRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(resetRequest.request.body.query).toContain('mutation RequestPasswordReset');
    expect(resetRequest.request.body.variables.input).toEqual({
      email: 'operator@authstarter.local',
    });
    resetRequest.flush({
      data: {
        requestPasswordReset: {
          message: 'If a matching active account exists, a password reset email has been sent.',
        },
      },
    });

    const result = await requestReset;

    expect(result.message).toContain('password reset email');
  });

  it('resets a password through the backend GraphQL mutation', async () => {
    const resetPassword = firstValueFrom(
      service.resetPassword({
        token: 'opaque-reset-token',
        newPassword: 'new-db-backed-password',
      }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });

    const resetRequest = httpTesting.expectOne(environment.graphql.endpoint);
    expect(resetRequest.request.body.query).toContain('mutation ResetPassword');
    expect(resetRequest.request.body.variables.input).toEqual({
      token: 'opaque-reset-token',
      newPassword: 'new-db-backed-password',
    });
    resetRequest.flush({
      data: {
        resetPassword: {
          message: 'Password has been reset. You can now sign in.',
        },
      },
    });

    const result = await resetPassword;

    expect(result.message).toContain('sign in');
  });

  it('fetches the configured OAuth2 providers from the backend', async () => {
    const fetchProviders = firstValueFrom(service.fetchOAuth2Providers());

    httpTesting
      .expectOne(`${environment.backendBaseUrl}/auth/oauth2/providers`)
      .flush([{ id: 'google', label: 'Google', authorizationUrl: '/oauth2/authorization/google' }]);

    const providers = await fetchProviders;

    expect(providers).toEqual([
      { id: 'google', label: 'Google', authorizationUrl: '/oauth2/authorization/google' },
    ]);
    expect(service.oauth2AuthorizationUrl(providers[0])).toBe(
      `${environment.backendBaseUrl}/oauth2/authorization/google`,
    );
  });

  it('returns no OAuth2 providers when the discovery endpoint fails', async () => {
    const fetchProviders = firstValueFrom(service.fetchOAuth2Providers());

    httpTesting
      .expectOne(`${environment.backendBaseUrl}/auth/oauth2/providers`)
      .flush('unavailable', { status: 503, statusText: 'Service Unavailable' });

    expect(await fetchProviders).toEqual([]);
  });

  it('clears stale organization context when a session expires', async () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    const login = firstValueFrom(
      service.login({ email: 'operator@authstarter.local', password: 'authstarter-local-password' }),
    );

    httpTesting.expectOne(`${environment.backendBaseUrl}/auth/csrf`).flush({
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
      token: 'csrf-token',
    });
    httpTesting.expectOne(environment.graphql.endpoint).flush({
      data: {
        login: {
          authenticated: true,
          mustChangePassword: false,
          principal: {
            id: 'baseline-operator',
            email: 'operator@authstarter.local',
            displayName: 'Baseline Operator',
            roles: ['ORG_ADMIN'],
            mustChangePassword: false,
          },
        },
      },
    });
    httpTesting.expectOne(environment.graphql.endpoint).flush(currentUserProfileResponse('ORG_ADMIN'));

    await login;
    expect(service.currentOrganization()?.organizationDisplayName).toBe('Auth Starter Local');

    service.handleUnauthorized();

    expect(service.status()).toBe('expired');
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.currentOrganization()).toBeNull();
    expect(router.navigate).toHaveBeenCalledOnceWith(['/login'], { queryParams: { reason: 'expired' } });
  });

  function currentUserProfileResponse(role = 'SUPERADMIN') {
    return {
      data: {
        currentUserProfile: {
          id: '30000000-0000-4000-8000-000000000001',
          email: 'operator@authstarter.local',
          displayName: 'Baseline Operator',
          status: 'ACTIVE',
          currentOrganization: {
            organizationId: '20000000-0000-4000-8000-000000000001',
            organizationDisplayName: 'Auth Starter Local',
            organizationStatus: 'ACTIVE',
            workspaceId: '10000000-0000-4000-8000-000000000001',
            workspaceCode: 'local-authstarter',
            workspaceStatus: 'ACTIVE',
            role,
          },
          memberships: [
            {
              organizationId: '20000000-0000-4000-8000-000000000001',
              organizationDisplayName: 'Auth Starter Local',
              organizationStatus: 'ACTIVE',
              workspaceId: '10000000-0000-4000-8000-000000000001',
              workspaceCode: 'local-authstarter',
              role,
              status: 'ACTIVE',
              primaryMembership: true,
            },
          ],
        },
      },
    };
  }
});
