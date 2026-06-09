/**
 * Deterministic local credentials seeded by the backend `local` profile
 * (see backend/src/main/resources/application-local.yml and docker-compose.yml).
 * These are local-only demo credentials and must never be used in deployed
 * environments.
 */
export interface TestUser {
  email: string;
  password: string;
}

export const BASELINE_OPERATOR: TestUser = {
  email: 'operator@authstarter.local',
  password: 'authstarter-local-password',
};

/** Organization administrator authorized for the user-management area. */
export const ORG_ADMIN_USER: TestUser = {
  email: 'org-admin@authstarter.local',
  password: 'authstarter-local-password',
};

/** Standard member without access to the user-management area. */
export const STANDARD_USER: TestUser = {
  email: 'user@authstarter.local',
  password: 'authstarter-local-password',
};

/**
 * Password used when accepting an invitation during end-to-end tests. Must meet
 * the application's password policy (12-128 characters).
 */
export const INVITED_USER_PASSWORD = 'invited-user-local-password';
