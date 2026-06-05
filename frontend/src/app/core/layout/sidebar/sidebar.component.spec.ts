import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../auth/auth.service';
import { SidebarComponent } from './sidebar.component';

describe('SidebarComponent', () => {
  let fixture: ComponentFixture<SidebarComponent>;
  let authService: jasmine.SpyObj<Pick<AuthService, 'hasAnyRole'>>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<Pick<AuthService, 'hasAnyRole'>>('AuthService', ['hasAnyRole']);
    authService.hasAnyRole.and.callFake((roles) => !roles.includes('SUPERADMIN'));

    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();
  });

  it('hides role-restricted navigation entries when the session lacks the role', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Dashboard');
    expect(text).toContain('Workspace');
    expect(text).toContain('Notifications');
    expect(text).toContain('Account');
    expect(text).not.toContain('User Management');
  });
});
