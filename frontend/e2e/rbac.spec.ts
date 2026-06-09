import { expect, test } from '@playwright/test';

import { ORG_ADMIN_USER, STANDARD_USER } from './fixtures/users';
import { login } from './helpers';

test.describe('role-based access control', () => {
  test('allows an authorized role into the user-management area', async ({ page }) => {
    await login(page, ORG_ADMIN_USER);

    await page.goto('/app/admin');

    await expect(page).toHaveURL(/\/app\/admin$/);
    await expect(page.getByRole('heading', { name: 'Account lifecycle' })).toBeVisible();
  });

  test('redirects an unauthorized role to the not-authorized page', async ({ page }) => {
    await login(page, STANDARD_USER);

    await page.goto('/app/admin');

    await expect(page).toHaveURL(/\/app\/not-authorized$/);
    await expect(page.getByRole('heading', { name: 'Not authorized' })).toBeVisible();
  });
});
