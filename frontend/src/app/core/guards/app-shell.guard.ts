import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

export const appShellGuard: CanMatchFn = (_route, segments) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated() && !authService.mustChangePassword()) {
    return true;
  }

  const returnUrl = `/${segments.map((segment) => segment.path).join('/')}`;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl } });
};
