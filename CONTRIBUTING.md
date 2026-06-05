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

## Development

```sh
cd backend && ./gradlew test
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 build
```

## Pull Requests

- Keep changes focused.
- Include tests for behavior changes.
- Update README or `.env.example` when configuration changes.
- Never commit secrets, real URLs, user data, or deployment-specific values.
