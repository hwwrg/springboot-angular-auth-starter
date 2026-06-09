import { expect, test } from '@playwright/test';

import { INVITED_USER_PASSWORD } from './fixtures/users';
import { createInvitedUser, fetchInvitationLink, login } from './helpers';

test.describe('invitation and first-login', () => {
  test('accepts an invitation and signs in for the first time', async ({ page, request }) => {
    const email = await createInvitedUser(request);
    const setupLink = await fetchInvitationLink(request, email);

    // Invitation acceptance: set the initial password through the setup page.
    await page.goto(setupLink);
    await page.getByLabel('New password', { exact: true }).fill(INVITED_USER_PASSWORD);
    await page.getByRole('button', { name: 'Set password' }).click();
    await expect(page.getByRole('status')).toContainText('Password setup is complete');

    // First login: the newly invited user signs in and reaches the dashboard.
    await login(page, { email, password: INVITED_USER_PASSWORD });
    await expect(page).toHaveURL(/\/app\/dashboard$/);
  });
});
