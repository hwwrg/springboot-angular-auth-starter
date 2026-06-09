# Security Policy

## Supported Versions

This starter is pre-1.0. Security fixes are applied to the current default branch.

## Reporting a Vulnerability

Do not open a public issue for a suspected vulnerability.

**Preferred:** Use [GitHub private security advisories](https://github.com/hwwrg/springboot-angular-auth-starter/security/advisories/new) to report confidentially.

**Alternative:** Email the maintainer or project owner listed for your fork or distribution.

Include:

- Affected commit or release
- Reproduction steps
- Impact assessment
- Any relevant logs with secrets removed

We aim to acknowledge reports within 5 business days.

## Security Notes

- Local defaults in `.env.example` are dummy development values.
- The included email providers are local mock and generic SMTP; `.env.example` uses dummy local values.
- Invitation and reset tokens are stored as hashes.
- The frontend does not store session tokens or JWTs in local storage.
- Review cookie, CORS, password policy, email provider, rate limiting, monitoring, and retention settings before deploying a derived application.
