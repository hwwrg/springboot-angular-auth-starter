import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

export const guestShellGuard: CanMatchFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated() && !authService.mustChangePassword()
    ? router.createUrlTree(['/app/dashboard'])
    : true;
};
