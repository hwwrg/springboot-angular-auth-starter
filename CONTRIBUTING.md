# Contributing

Thanks for contributing to `springboot-angular-auth-starter`.

## Scope

Keep this project generic. Do not add application-specific workflows, real domains, deployment values, secrets, or environment-specific infrastructure details.

Good contributions include:

- Authentication and session hardening
- RBAC improvements
- User lifecycle and password lifecycle improvements
- Generic notification provider abstractions
- Tests and documentation for the retained starter features

## Getting started

1. For significant changes, open an issue first to discuss your approach before writing code.
2. Fork the repository and create a branch from `main`.
3. Follow the local setup in the [README](./README.md#local-setup).
4. Make your changes in a focused, single-purpose branch.
5. Run the relevant verification commands before opening a PR.

## Development

```sh
cd backend && ./gradlew check
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

Or from the repo root:

```sh
make verify
```

## Pull requests

- Keep changes focused and single-purpose.
- Include tests for behavior changes.
- Update README or `.env.example` when configuration changes.
- Database changes must use additive Flyway migrations — never edit applied migrations.
- Never commit secrets, real URLs, user data, or deployment-specific values.
- Fill in the PR template fields.

## Reporting issues

Use the [GitHub issue tracker](https://github.com/woodyhua/springboot-angular-auth-starter/issues).
For security issues, follow [SECURITY.md](./SECURITY.md) instead.

## Code style

- Java: standard Spring Boot conventions; the Gradle `check` task runs linting.
- TypeScript/Angular: ESLint rules enforced by `pnpm lint`.
- Keep code comments in English.
- Prefer small, reviewable changes over large refactors.

## License

By contributing, you agree that your contributions will be licensed under the [Apache 2.0 License](./LICENSE).
