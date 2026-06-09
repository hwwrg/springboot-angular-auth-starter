import { expect, type Page } from '@playwright/test';

import type { TestUser } from './fixtures/users';

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
