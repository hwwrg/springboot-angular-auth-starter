import { expect, test } from '@playwright/test';

import { createActivatedUser, fetchPasswordResetLink, login } from './helpers';

const NEW_PASSWORD = 'reset-user-local-password';

test.describe('password recovery', () => {
  test('requests a reset link and signs in with the new password', async ({ page, request }) => {
    const user = await createActivatedUser(request);

    // Forgot password: reach the request form from the login page.
    await page.goto('/login');
    await page.getByRole('link', { name: 'Forgot password?' }).click();
    await expect(page).toHaveURL(/\/forgot-password$/);

    await page.getByRole('textbox', { name: 'Email' }).fill(user.email);
    await page.getByRole('button', { name: 'Send reset link' }).click();
    await expect(page.getByRole('status')).toContainText('password reset email has been sent');

    // Reset password: follow the emailed link and choose a new password.
    const resetLink = await fetchPasswordResetLink(request, user.email);
    await page.goto(resetLink);
    await page.getByRole('textbox', { name: /^New password/ }).fill(NEW_PASSWORD);
    await page.getByRole('textbox', { name: /^Confirm new password/ }).fill(NEW_PASSWORD);
    await page.getByRole('button', { name: 'Reset password' }).click();
    await expect(page.getByRole('status')).toContainText('Password has been reset');

    // The new credentials grant access to the dashboard.
    await login(page, { email: user.email, password: NEW_PASSWORD });
    await expect(page).toHaveURL(/\/app\/dashboard$/);
  });
});
