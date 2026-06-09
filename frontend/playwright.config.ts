import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end configuration for the Angular frontend.
 *
 * The backend and PostgreSQL are expected to be running via Docker Compose
 * (`docker compose up --build` from the repository root) before the suite
 * starts. Playwright launches the Angular dev server on port 4200 itself.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:4200';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'pnpm start',
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
