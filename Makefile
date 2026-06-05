.PHONY: verify verify-backend verify-frontend backend-test backend-build frontend-lint frontend-test frontend-build

PNPM := npx -y pnpm@10.6.5

verify: verify-backend verify-frontend

verify-backend: backend-test backend-build

verify-frontend: frontend-lint frontend-test frontend-build

backend-test:
	cd backend && ./gradlew test

backend-build:
	cd backend && ./gradlew bootJar

frontend-lint:
	cd frontend && $(PNPM) lint

frontend-test:
	cd frontend && $(PNPM) test

frontend-build:
	cd frontend && $(PNPM) build
