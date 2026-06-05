import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthRole } from '../auth/auth.model';
import { AuthService } from '../auth/auth.service';

export const roleAccessGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const allowedRoles = (route.data?.['roles'] ?? []) as AuthRole[];

  if (authService.hasAnyRole(allowedRoles)) {
    return true;
  }

  return router.createUrlTree(['/app/not-authorized']);
};
