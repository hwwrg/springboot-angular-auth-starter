import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router } from '@angular/router';

import { AuthRole } from '../auth/auth.model';
import { AuthService } from '../auth/auth.service';
import { roleAccessGuard } from './role-access.guard';

describe('roleAccessGuard', () => {
  let authService: jasmine.SpyObj<Pick<AuthService, 'hasAnyRole'>>;
  let router: Router;

  beforeEach(() => {
    authService = jasmine.createSpyObj<Pick<AuthService, 'hasAnyRole'>>('AuthService', ['hasAnyRole']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('allows navigation when the current session has an allowed role', () => {
    authService.hasAnyRole.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      roleAccessGuard(routeWithRoles(['SUPERADMIN', 'ORG_ADMIN']), {} as never),
    );

    expect(result).toBeTrue();
    expect(authService.hasAnyRole).toHaveBeenCalledWith(['SUPERADMIN', 'ORG_ADMIN']);
  });

  it('redirects to not-authorized when the session lacks the required role', () => {
    authService.hasAnyRole.and.returnValue(false);
    spyOn(router, 'createUrlTree').and.callThrough();

    const result = TestBed.runInInjectionContext(() =>
      roleAccessGuard(routeWithRoles(['SUPERADMIN']), {} as never),
    );

    expect(router.createUrlTree).toHaveBeenCalledWith(['/app/not-authorized']);
    expect(result.toString()).toBe('/app/not-authorized');
  });

});

function routeWithRoles(roles: AuthRole[]): ActivatedRouteSnapshot {
  return { data: { roles } } as unknown as ActivatedRouteSnapshot;
}
