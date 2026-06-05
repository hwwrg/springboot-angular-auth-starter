import { HttpInterceptorFn } from '@angular/common/http';

function createCorrelationId(): string {
  return `fe-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export const correlationIdInterceptor: HttpInterceptorFn = (request, next) =>
  next(
    request.clone({
      setHeaders: {
        'X-Correlation-Id': request.headers.get('X-Correlation-Id') ?? createCorrelationId(),
      },
    }),
  );

