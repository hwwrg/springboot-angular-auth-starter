<!-- Thanks for contributing! Keep PRs focused and single-purpose. -->

## Summary

<!-- What does this change do, and why? -->

## Related issue

<!-- e.g. Closes #123 -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Documentation
- [ ] Tests
- [ ] Refactor / chore

## Verification

How did you verify this change? Check all that apply:

- [ ] `cd backend && ./gradlew check`
- [ ] `cd frontend && npx -y pnpm@10.6.5 lint`
- [ ] `cd frontend && npx -y pnpm@10.6.5 test`
- [ ] `cd frontend && npx -y pnpm@10.6.5 build`
- [ ] Manually tested the affected flow

## Checklist

- [ ] Change is focused and single-purpose
- [ ] Added or updated tests for behavior changes
- [ ] Updated docs / `.env.example` if configuration changed
- [ ] No secrets, real URLs, or environment-specific values committed
- [ ] New DB changes are additive Flyway migrations (no edits to applied migrations)
