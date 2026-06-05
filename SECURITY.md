# Security Policy

## Supported Versions

This starter is pre-1.0. Security fixes are applied to the current default branch.

## Reporting A Vulnerability

Do not open a public issue for a suspected vulnerability. Email the maintainer or project owner listed for your fork/distribution, or use the private advisory workflow if this repository is hosted on GitHub.

Include:

- Affected commit or release
- Reproduction steps
- Impact assessment
- Any relevant logs with secrets removed

## Security Notes

- Local defaults in `.env.example` are dummy development values.
- The included email providers are local mock and generic SMTP; `.env.example` uses dummy local values.
- Invitation and reset tokens are stored as hashes.
- The frontend does not store session tokens or JWTs in local storage.
- Review cookie, CORS, password policy, email provider, rate limiting, monitoring, and retention settings before deploying a derived application.
