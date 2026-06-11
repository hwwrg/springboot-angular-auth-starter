import { expect, type APIRequestContext, type Page } from '@playwright/test';

import { BASELINE_OPERATOR, INVITED_USER_PASSWORD, type TestUser } from './fixtures/users';

const BACKEND_URL = process.env.E2E_BACKEND_URL ?? 'http://localhost:8080';
const MAILPIT_URL = process.env.E2E_MAILPIT_URL ?? 'http://localhost:8025';

const LOGIN_MUTATION = `
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      authenticated
    }
  }
`;

const ADMIN_CREATE_USER_MUTATION = `
  mutation AdminCreateUser($input: CreateAdminUserInput!) {
    adminCreateUser(input: $input) {
      id
      email
      status
    }
  }
`;

const ACCEPT_USER_INVITE_MUTATION = `
  mutation AcceptUserInvite($input: AcceptUserInviteInput!) {
    acceptUserInvite(input: $input) {
      userId
      email
      status
    }
  }
`;

interface CsrfPayload {
  headerName: string;
  token: string;
}

/**
 * Sign in through the login page using accessible selectors and wait until the
 * authenticated dashboard has loaded.
 */
export async function login(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login');

  await page.getByRole('textbox', { name: 'Email' }).fill(user.email);
  await page.getByLabel('Password', { exact: true }).fill(user.password);
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL(/\/app\/dashboard$/);
  await expect(page.getByRole('heading', { name: 'Authentication starter' })).toBeVisible();
}

/**
 * Execute a GraphQL operation against the backend, attaching a freshly fetched
 * CSRF token. Cookies (including the session) are shared by the request context
 * across calls.
 */
async function graphql<T>(
  request: APIRequestContext,
  query: string,
  variables: Record<string, unknown>,
): Promise<T> {
  const csrf: CsrfPayload = await (await request.get(`${BACKEND_URL}/auth/csrf`)).json();
  const response = await request.post(`${BACKEND_URL}/graphql`, {
    headers: { 'content-type': 'application/json', [csrf.headerName]: csrf.token },
    data: { query, variables },
  });
  const body = (await response.json()) as { data?: T; errors?: { message?: string }[] };
  if (body.errors?.length) {
    throw new Error(body.errors[0].message ?? 'GraphQL request failed.');
  }
  if (!body.data) {
    throw new Error('GraphQL response did not include data.');
  }
  return body.data;
}

/**
 * Sign in as an administrator with an active organization context and create an
 * invited user. Returns the invited user's email. The email is unique per run
 * so repeated executions do not collide on an already-consumed invitation.
 */
export async function createInvitedUser(request: APIRequestContext): Promise<string> {
  await graphql(request, LOGIN_MUTATION, {
    input: { email: BASELINE_OPERATOR.email, password: BASELINE_OPERATOR.password },
  });

  const email = `invitee+${Date.now()}@authstarter.local`;
  await graphql(request, ADMIN_CREATE_USER_MUTATION, {
    input: {
      email,
      displayName: 'Invited Member',
      userStatus: 'INVITED',
      role: 'USER',
      membershipStatus: 'INVITED',
      primaryMembership: true,
    },
  });

  return email;
}

/**
 * Poll the local Mailpit mail catcher for an email addressed to the given
 * recipient whose body contains a link matching the pattern, and return that
 * link. All matching messages are scanned because a recipient may receive
 * several emails during a test (for example an invitation and then a reset).
 */
async function fetchEmailedLink(
  request: APIRequestContext,
  email: string,
  linkPattern: RegExp,
  description: string,
): Promise<string> {
  const deadline = Date.now() + 15_000;
  while (Date.now() < deadline) {
    const search = await request.get(`${MAILPIT_URL}/api/v1/search`, {
      params: { query: `to:${email}` },
    });
    if (search.ok()) {
      const { messages } = (await search.json()) as { messages?: { ID: string }[] };
      for (const { ID } of messages ?? []) {
        const detail = await request.get(`${MAILPIT_URL}/api/v1/message/${ID}`);
        const message = (await detail.json()) as { Text?: string; HTML?: string };
        const content = `${message.Text ?? ''} ${message.HTML ?? ''}`;
        const match = content.match(linkPattern);
        if (match) {
          return match[0];
        }
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }

  throw new Error(`No email with a ${description} link was found for ${email}.`);
}

/**
 * Poll the local Mailpit mail catcher for the invitation email addressed to the
 * given recipient and return the password-setup link it contains.
 */
export async function fetchInvitationLink(
  request: APIRequestContext,
  email: string,
): Promise<string> {
  return fetchEmailedLink(
    request,
    email,
    /https?:\/\/[^\s"'<]*\/accept-invite\?token=[^\s"'<]+/,
    'password setup',
  );
}

/**
 * Poll the local Mailpit mail catcher for the password reset email addressed to
 * the given recipient and return the reset link it contains.
 */
export async function fetchPasswordResetLink(
  request: APIRequestContext,
  email: string,
): Promise<string> {
  return fetchEmailedLink(
    request,
    email,
    /https?:\/\/[^\s"'<]*\/reset-password\?token=[^\s"'<]+/,
    'password reset',
  );
}

/**
 * Create an invited user and accept the invitation through the API so the
 * account is active with a known password. Used by tests that need a
 * disposable active account (for example password reset) without exercising
 * the invitation UI, which has its own coverage.
 */
export async function createActivatedUser(request: APIRequestContext): Promise<TestUser> {
  const email = await createInvitedUser(request);
  const setupLink = await fetchInvitationLink(request, email);
  const token = new URL(setupLink).searchParams.get('token');
  if (!token) {
    throw new Error(`The invitation link for ${email} did not include a token.`);
  }

  await graphql(request, ACCEPT_USER_INVITE_MUTATION, {
    input: { token, newPassword: INVITED_USER_PASSWORD },
  });

  return { email, password: INVITED_USER_PASSWORD };
}
