# Frontend

Angular frontend for `springboot-angular-auth-starter`.

## Run

```sh
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

Open `http://localhost:4200`.

## Verify

```sh
npx -y pnpm@10.6.5 lint
npx -y pnpm@10.6.5 build
```

## Scope

Kept: login, logout, session bootstrap, route guards, account context, password change, invite acceptance, forgot/reset password, notification history, and admin user management.

Out of scope: application-specific UI modules, deployment infrastructure, and generated runtime artifacts.
