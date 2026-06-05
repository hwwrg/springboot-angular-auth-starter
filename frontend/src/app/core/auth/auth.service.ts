import { HttpClient } from '@angular/common/http';
import { computed, Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, firstValueFrom, map, Observable, of, switchMap, tap, throwError } from 'rxjs';

import { CurrentUserProfile } from '../context/organization-user-context.model';
import { RuntimeConfigService } from '../runtime-config/runtime-config.service';
import {
  AcceptUserInviteInput,
  AuthRole,
  AuthSession,
  ChangeOwnPasswordInput,
  CsrfTokenPayload,
  InvitationPasswordSetup,
  LoginCredentials,
  PasswordReset,
  PasswordResetCompleteInput,
  PasswordResetRequestInput,
} from './auth.model';

interface GraphQlError {
  message?: string;
}

interface GraphQlResponse<T> {
  data?: T;
  errors?: GraphQlError[];
}

type AuthStatus = 'checking' | 'ready' | 'expired';

interface AuthSessionState extends AuthSession {
  status: AuthStatus;
}

const ANONYMOUS_SESSION: AuthSessionState = {
  status: 'ready',
  authenticated: false,
  principal: null,
  mustChangePassword: false,
};

const CURRENT_SESSION_QUERY = `
  query CurrentSession {
    currentSession {
      authenticated
      mustChangePassword
      principal {
        id
        email
        displayName
        roles
        mustChangePassword
      }
    }
  }
`;

const CURRENT_USER_PROFILE_QUERY = `
  query CurrentUserProfile {
    currentUserProfile {
      id
      email
      displayName
      status
      currentOrganization {
        organizationId
        organizationDisplayName
        organizationStatus
        workspaceId
        workspaceCode
        workspaceStatus
        role
      }
      memberships {
        organizationId
        organizationDisplayName
        organizationStatus
        workspaceId
        workspaceCode
        role
        status
        primaryMembership
      }
    }
  }
`;

const LOGIN_MUTATION = `
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      authenticated
      mustChangePassword
      principal {
        id
        email
        displayName
        roles
        mustChangePassword
      }
    }
  }
`;

const LOGOUT_MUTATION = `
  mutation Logout {
    logout {
      authenticated
      mustChangePassword
      principal {
        id
        email
        displayName
        roles
        mustChangePassword
      }
    }
  }
`;

const CHANGE_OWN_PASSWORD_MUTATION = `
  mutation ChangeOwnPassword($input: ChangeOwnPasswordInput!) {
    changeOwnPassword(input: $input) {
      authenticated
      mustChangePassword
      principal {
        id
        email
        displayName
        roles
        mustChangePassword
      }
    }
  }
`;

const ACCEPT_USER_INVITE_MUTATION = `
  mutation AcceptUserInvite($input: AcceptUserInviteInput!) {
    acceptUserInvite(input: $input) {
      email
      status
    }
  }
`;

const REQUEST_PASSWORD_RESET_MUTATION = `
  mutation RequestPasswordReset($input: PasswordResetRequestInput!) {
    requestPasswordReset(input: $input) {
      message
    }
  }
`;

const RESET_PASSWORD_MUTATION = `
  mutation ResetPassword($input: PasswordResetCompleteInput!) {
    resetPassword(input: $input) {
      message
    }
  }
`;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly runtimeConfigService = inject(RuntimeConfigService);
  private readonly sessionState = signal<AuthSessionState>({
    status: 'checking',
    authenticated: false,
    principal: null,
    mustChangePassword: false,
  });
  private readonly csrfTokenState = signal<CsrfTokenPayload | null>(null);
  private readonly userProfileState = signal<CurrentUserProfile | null>(null);

  readonly session = computed<AuthSession>(() => ({
    authenticated: this.sessionState().authenticated,
    principal: this.sessionState().principal,
    mustChangePassword: this.sessionState().mustChangePassword,
  }));
  readonly status = computed(() => this.sessionState().status);
  readonly isAuthenticated = computed(() => this.sessionState().authenticated);
  readonly roles = computed(() => this.sessionState().principal?.roles ?? []);
  readonly mustChangePassword = computed(() => this.sessionState().mustChangePassword);
  readonly csrfToken = computed(() => this.csrfTokenState());
  readonly userProfile = computed(() => this.userProfileState());
  readonly currentOrganization = computed(() => this.userProfileState()?.currentOrganization ?? null);
  readonly sessionLabel = computed(() => {
    const principal = this.sessionState().principal;
    const organizationContext = this.currentOrganization();
    if (!principal) {
      return 'Anonymous session';
    }

    return organizationContext
      ? `${principal.displayName} · ${organizationContext.organizationDisplayName}`
      : `${principal.displayName} · ${principal.roles.join(', ')}`;
  });

  bootstrapSession(): Promise<void> {
    if (!this.runtimeConfigService.requiresRemoteConfig()) {
      return this.bootstrapBackendSession();
    }

    return this.runtimeConfigService.load().then(() => this.bootstrapBackendSession());
  }

  private bootstrapBackendSession(): Promise<void> {
    return firstValueFrom(
      this.ensureCsrfToken().pipe(
        switchMap(() => this.currentSession()),
        switchMap((session) =>
          session.authenticated && !session.mustChangePassword ? this.loadCurrentUserProfile() : of(undefined),
        ),
        catchError(() => {
          this.sessionState.set(ANONYMOUS_SESSION);
          this.userProfileState.set(null);
          return of(undefined);
        }),
        map(() => undefined),
      ),
    );
  }

  login(credentials: LoginCredentials): Observable<AuthSession> {
    return this.ensureCsrfToken().pipe(
      switchMap(() =>
        this.graphql<{ login: AuthSession }>(LOGIN_MUTATION, {
          input: {
            email: credentials.email,
            password: credentials.password,
          },
        }),
      ),
      map((response) => response.login),
      switchMap((session) => {
        this.applySession(session);
        return session.authenticated && !session.mustChangePassword
          ? this.loadCurrentUserProfile().pipe(
              map(() => session),
              catchError(() => of(session)),
            )
          : of(session);
      }),
    );
  }

  logout(): Observable<void> {
    return this.ensureCsrfToken().pipe(
      switchMap(() => this.graphql<{ logout: AuthSession }>(LOGOUT_MUTATION)),
      tap((response) => this.applySession(response.logout)),
      tap(() => this.userProfileState.set(null)),
      map(() => undefined),
      catchError((error: unknown) => {
        this.sessionState.set(ANONYMOUS_SESSION);
        this.userProfileState.set(null);
        return throwError(() => error);
      }),
    );
  }

  changeOwnPassword(input: ChangeOwnPasswordInput): Observable<AuthSession> {
    return this.ensureCsrfToken().pipe(
      switchMap(() => this.graphql<{ changeOwnPassword: AuthSession }>(CHANGE_OWN_PASSWORD_MUTATION, { input })),
      map((response) => response.changeOwnPassword),
      switchMap((session) => {
        this.applySession(session);
        return session.authenticated && !session.mustChangePassword
          ? this.loadCurrentUserProfile().pipe(
              map(() => session),
              catchError(() => of(session)),
            )
          : of(session);
      }),
    );
  }

  acceptUserInvite(input: AcceptUserInviteInput): Observable<InvitationPasswordSetup> {
    return this.ensureCsrfToken().pipe(
      switchMap(() =>
        this.graphql<{ acceptUserInvite: InvitationPasswordSetup }>(ACCEPT_USER_INVITE_MUTATION, { input }),
      ),
      map((response) => response.acceptUserInvite),
    );
  }

  requestPasswordReset(input: PasswordResetRequestInput): Observable<PasswordReset> {
    return this.ensureCsrfToken().pipe(
      switchMap(() =>
        this.graphql<{ requestPasswordReset: PasswordReset }>(REQUEST_PASSWORD_RESET_MUTATION, { input }),
      ),
      map((response) => response.requestPasswordReset),
    );
  }

  resetPassword(input: PasswordResetCompleteInput): Observable<PasswordReset> {
    return this.ensureCsrfToken().pipe(
      switchMap(() => this.graphql<{ resetPassword: PasswordReset }>(RESET_PASSWORD_MUTATION, { input })),
      map((response) => response.resetPassword),
    );
  }

  handleUnauthorized(): void {
    if (this.sessionState().authenticated) {
      this.sessionState.set({ ...ANONYMOUS_SESSION, status: 'expired' });
    }

    this.userProfileState.set(null);
    this.router.navigate(['/login'], { queryParams: { reason: 'expired' } });
  }

  hasAnyRole(allowedRoles: readonly AuthRole[]): boolean {
    if (!allowedRoles.length) {
      return true;
    }

    const currentRoles = new Set(this.roles());
    return allowedRoles.some((role) => currentRoles.has(role));
  }

  refreshCsrfToken(): Observable<CsrfTokenPayload> {
    return this.http
      .get<CsrfTokenPayload>(`${this.runtimeConfig().backendBaseUrl}/auth/csrf`, { withCredentials: true })
      .pipe(tap((payload) => this.csrfTokenState.set(payload)));
  }

  private ensureCsrfToken(): Observable<CsrfTokenPayload> {
    const csrfToken = this.csrfTokenState();
    return csrfToken ? of(csrfToken) : this.refreshCsrfToken();
  }

  private currentSession(): Observable<AuthSession> {
    return this.graphql<{ currentSession: AuthSession }>(CURRENT_SESSION_QUERY).pipe(
      map((response) => response.currentSession),
      tap((session) => this.applySession(session)),
    );
  }

  private loadCurrentUserProfile(): Observable<void> {
    return this.graphql<{ currentUserProfile: CurrentUserProfile | null }>(CURRENT_USER_PROFILE_QUERY).pipe(
      map((response) => response.currentUserProfile),
      tap((profile) => this.userProfileState.set(profile)),
      map(() => undefined),
    );
  }

  private graphql<T>(query: string, variables?: Record<string, unknown>): Observable<T> {
    return this.http
      .post<GraphQlResponse<T>>(
        this.runtimeConfig().graphql.endpoint,
        {
          query,
          variables,
        },
        { withCredentials: true },
      )
      .pipe(
        map((response) => {
          if (response.errors?.length) {
            throw new Error(response.errors[0].message ?? 'Authentication request failed.');
          }

          if (!response.data) {
            throw new Error('Authentication response did not include data.');
          }

          return response.data;
        }),
      );
  }

  private runtimeConfig() {
    return this.runtimeConfigService.config();
  }

  private applySession(session: AuthSession): void {
    this.sessionState.set({
      status: 'ready',
      authenticated: session.authenticated,
      principal: session.principal,
      mustChangePassword: session.mustChangePassword,
    });

    if (!session.authenticated || session.mustChangePassword) {
      this.userProfileState.set(null);
    }
  }
}
