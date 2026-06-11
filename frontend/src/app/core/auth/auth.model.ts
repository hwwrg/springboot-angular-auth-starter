export type AuthRole = 'SUPERADMIN' | 'ORG_ADMIN' | 'USER';

export interface AuthPrincipal {
  id: string;
  email: string;
  displayName: string;
  roles: AuthRole[];
  mustChangePassword: boolean;
}

export interface AuthSession {
  authenticated: boolean;
  principal: AuthPrincipal | null;
  mustChangePassword: boolean;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface ChangeOwnPasswordInput {
  currentPassword?: string;
  newPassword: string;
}

export interface AcceptUserInviteInput {
  token: string;
  newPassword: string;
}

export interface PasswordResetRequestInput {
  email: string;
}

export interface PasswordResetCompleteInput {
  token: string;
  newPassword: string;
}

export interface PasswordReset {
  message: string;
}

export interface InvitationPasswordSetup {
  email: string;
  status: string;
}

export interface OAuth2Provider {
  id: string;
  label: string;
  authorizationUrl: string;
}

export interface CsrfTokenPayload {
  headerName: string;
  parameterName: string;
  token: string;
}
