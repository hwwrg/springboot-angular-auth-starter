# springboot-angular-auth-starter

Open-source Spring Boot + Angular authentication starter.

- Backend: Spring Boot
- Frontend: Angular
- Keep comments in English
- Prefer small, focused changes
- Never commit secrets
- Validate `docker compose config` and relevant builds/tests before committing

## Development workflow

For each meaningful change, follow this workflow:

- New task = new dedicated Git branch.
- Complex task = planning first, then implementation.
- Each implementation slice = small focused change + relevant validation.
- Validation passes = commit.
- Branch complete = provide the exact `gh pr create` command.
- Do not run `gh pr create` automatically unless explicitly requested.
- After a PR is merged, return to `main`, pull latest changes, then start the next task.

## Starting a new task

For every new feature, bug fix, documentation update, or meaningful change:

1. Start from latest local `main` unless explicitly told otherwise.

   ```bash
   git checkout main
   git pull
   git status --short
   ```

2. Check the working tree before editing.

   ```bash
   git status --short
   git branch --show-current
   ```

3. If the working tree is dirty, stop and explain what is dirty before making changes.

4. Create a dedicated branch before editing files. Use clear branch names:

   ```
   feature/<short-description>
   fix/<short-description>
   docs/<short-description>
   chore/<short-description>
   ```

5. Inspect relevant files before making changes.
6. Propose a short plan before editing.
7. Develop in small coherent slices.
8. Run relevant validation after each meaningful slice.
9. Commit only after validation passes.
10. Use clear conventional commit messages, for example:

    ```
    feat(auth): add login form validation
    fix(docker): add local database service
    docs(readme): document local setup
    chore(dev): document branch and validation workflow
    ```

## Planning complex tasks

Use a planning pass for medium or complex tasks, especially when the task affects:

- Spring Boot security
- authentication or authorization
- GraphQL schema or resolvers
- database schema or migrations
- Docker/local development setup
- Angular authentication flow
- tests or CI
- documentation structure
- public developer onboarding

When planning:

1. Do not edit files yet.
2. Explore the relevant code.
3. Identify likely files to change.
4. Identify backend impact.
5. Identify frontend impact.
6. Identify documentation impact.
7. Identify tests and validation commands.
8. Identify risks.
9. Split the implementation into small validated slices.
10. Wait for approval before editing.

## Long-task workflow

For long tasks, do not implement everything in one pass. Use this structure:

1. Create or update a short spec or implementation note if useful.
2. Confirm scope and out-of-scope.
3. Create a dedicated branch.
4. Implement slice by slice.
5. Validate each slice.
6. Stop if validation fails and explain the failure.
7. Commit only after validation passes.
8. At the end, summarize the branch and provide the PR command.

For very large features, prefer splitting into multiple PRs.

## Validation expectations

Choose validation commands based on the files changed. Common validation commands may include:

```bash
docker compose config
docker compose ps
docker compose logs --tail=100 backend
curl -fsS http://localhost:8080/actuator/health/readiness
```

- For backend changes, run the relevant Maven/Gradle build or tests.
- For frontend changes, run the relevant Angular lint/test/build commands.
- For documentation-only changes, validate formatting and links where practical.
- Never claim validation passed unless the commands were actually run successfully.

## End-of-task report

At the end of a branch task, always report:

- branch name
- files changed
- validation commands run
- validation result
- latest commit hash
- exact `gh pr create` command to run

Do not run `gh pr create` automatically unless explicitly requested.

## Safety rules

- Keep the task scope focused.
- Prefer small, reviewable changes.
- Do not refactor unrelated code.
- Do not overwrite unrelated local changes.
- Keep code comments in English.
- Never commit secrets, tokens, private keys, local credentials, `.env` files, or machine-specific files.
- Do not include private project details in this open-source repository.
