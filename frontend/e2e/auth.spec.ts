import { expect, test } from '@playwright/test';

import { BASELINE_OPERATOR } from './fixtures/users';
import { login } from './helpers';

test.describe('authentication smoke', () => {
  test('signs in successfully and lands on the dashboard', async ({ page }) => {
    await login(page, BASELINE_OPERATOR);

    await expect(page).toHaveURL(/\/app\/dashboard$/);
    await expect(page.getByRole('heading', { name: 'Authentication starter' })).toBeVisible();
  });

  test('redirects unauthenticated access to a protected route back to login', async ({ page }) => {
    await page.goto('/app/dashboard');

    await expect(page).toHaveURL(/\/login\?returnUrl=%2Fapp%2Fdashboard$/);
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  });

  test('signs out and clears the authenticated session', async ({ page }) => {
    await login(page, BASELINE_OPERATOR);

    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page).toHaveURL(/\/login$/);

    // The protected route should once again redirect to login.
    await page.goto('/app/dashboard');
    await expect(page).toHaveURL(/\/login\?returnUrl=%2Fapp%2Fdashboard$/);
  });
});
