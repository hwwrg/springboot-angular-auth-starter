# Documentation Maintenance

The root [README.md](../README.md) is the source of truth. Keep English canonical, then update translations.

## Current Documentation Set

- [English README](../README.md)
- [Simplified Chinese README](../README.zh-CN.md)
- [French README](../README.fr.md)
- [English docs](./en/README.md)
- [Simplified Chinese docs](./zh-CN/README.md)
- [French docs](./fr/README.md)

## Rules

- Update [README.md](../README.md) first.
- Keep [docs/en](./en/README.md) aligned with the English README.
- Update [README.zh-CN.md](../README.zh-CN.md), [README.fr.md](../README.fr.md), [docs/zh-CN](./zh-CN/README.md), and [docs/fr](./fr/README.md) after English changes.
- Preserve code examples, commands, environment variables, class names, API paths, file names, role names, package names, and script names in English.
- Do not document features that are not present in the repository.
- Validate relative Markdown links after documentation changes.
- Do not add a documentation framework unless a future issue explicitly asks for one.

## Before Committing Docs

Run a quick documentation review:

```sh
git status --short
```

Run safe project checks when relevant:

```sh
cd backend && ./gradlew test
cd frontend && npx -y pnpm@10.6.5 lint
```

Use broader checks before release documentation changes:

```sh
make verify
```
