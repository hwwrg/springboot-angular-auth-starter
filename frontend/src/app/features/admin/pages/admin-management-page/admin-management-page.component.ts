import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, type AbstractControl } from '@angular/forms';

import {
  AdminManagementBaseline,
  AdminUserManagementInput,
  AdminUserSummary,
  UpdateAdminUserManagementInput,
} from '../../../../core/admin/admin-management.model';
import { AdminManagementService } from '../../../../core/admin/admin-management.service';
import { EmptyStateComponent } from '../../../../shared/ui/empty-state/empty-state.component';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';

type UserLifecycleStatus = 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'ARCHIVED';
type UserRole = 'SUPERADMIN' | 'ORG_ADMIN' | 'USER';

@Component({
  selector: 'app-admin-management-page',
  imports: [EmptyStateComponent, ReactiveFormsModule, StatusPillComponent],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">User management</p>
        <h1>Account lifecycle</h1>
        <p>Create invited users, adjust roles, and inspect account email delivery.</p>
      </div>
      @if (baseline(); as baseline) {
        <app-status-pill [label]="baseline.currentOrganization.role" tone="ok" />
      } @else {
        <app-status-pill label="Unavailable" tone="warning" />
      }
    </section>

    @if (baseline(); as baseline) {
      <section class="admin-context-strip" aria-label="Admin context">
        <div>
          <span>Organization</span>
          <strong>{{ baseline.currentOrganization.organizationDisplayName }}</strong>
        </div>
        <div>
          <span>Boundary</span>
          <strong>{{ baseline.currentOrganization.workspaceCode }}</strong>
        </div>
        <div>
          <span>Users</span>
          <strong>{{ baseline.totals.userCount }}</strong>
        </div>
        <div>
          <span>Email events</span>
          <strong>{{ baseline.totals.notificationEventCount }}</strong>
        </div>
      </section>

      @if (saveMessage(); as message) {
        <section
          class="admin-save-message"
          [class.admin-save-message--error]="saveMessageTone() === 'error'"
          aria-live="polite"
        >
          {{ message }}
        </section>
      }

      @if (adminService.saveError(); as error) {
        <section class="admin-save-message admin-save-message--error" role="alert">{{ error }}</section>
      }

      <section class="management-forms" aria-label="User management form">
        <form class="management-form" [formGroup]="userForm" (ngSubmit)="saveUser(baseline)">
          <div class="management-form__header">
            <h2>{{ userForm.controls.id.value ? 'Edit user' : 'Invite user' }}</h2>
            <button type="button" (click)="resetUserForm()">New</button>
          </div>
          @if (!userForm.controls.id.value) {
            <p class="management-form__note">
              Creating a user sends a generic password setup notification through the local mock provider.
            </p>
          }
          <label>
            Email
            <input type="email" formControlName="email" [readonly]="!!userForm.controls.id.value" />
            @if (fieldError(userForm.controls.email, 'Email')) {
              <small class="field-error">{{ fieldError(userForm.controls.email, 'Email') }}</small>
            }
          </label>
          <label>
            Display name
            <input type="text" formControlName="displayName" />
            @if (fieldError(userForm.controls.displayName, 'Display name')) {
              <small class="field-error">{{ fieldError(userForm.controls.displayName, 'Display name') }}</small>
            }
          </label>
          <div class="management-form__pair">
            <label>
              User status
              <select formControlName="userStatus">
                @for (status of userStatuses; track status) {
                  <option [value]="status">{{ status }}</option>
                }
              </select>
            </label>
            <label>
              Role
              <select formControlName="role">
                @for (role of roleOptions(baseline); track role) {
                  <option [value]="role" [disabled]="!canSubmitRole(role, baseline)">{{ role }}</option>
                }
              </select>
              @if (!canSubmitRole(userForm.controls.role.value, baseline)) {
                <small class="field-error">Only a SUPERADMIN can assign SUPERADMIN.</small>
              }
            </label>
          </div>
          <div class="management-form__pair">
            <label>
              Membership
              <select formControlName="membershipStatus">
                @for (status of membershipStatuses; track status) {
                  <option [value]="status">{{ status }}</option>
                }
              </select>
            </label>
            <label class="management-form__checkbox">
              <input type="checkbox" formControlName="primaryMembership" />
              Primary context
            </label>
          </div>
          <button type="submit" [disabled]="adminService.saving()">Save user</button>
        </form>
      </section>

      <section class="admin-list-section">
        <div class="admin-section-heading">
          <h2>Users</h2>
          <span>{{ baseline.users.length }} total</span>
        </div>
        <div class="admin-table" role="table" aria-label="Managed users">
          @for (user of baseline.users; track user.id) {
            <button type="button" class="admin-table__row" (click)="editUser(user)">
              <span>
                <strong>{{ user.displayName }}</strong>
                <small>{{ user.email }}</small>
              </span>
              <app-status-pill [label]="user.role" tone="neutral" />
              <app-status-pill [label]="user.status" [tone]="user.status === 'ACTIVE' ? 'ok' : 'warning'" />
            </button>
          } @empty {
            <app-empty-state title="No users" body="Create the first invited user for this starter workspace." />
          }
        </div>
      </section>

      <section class="admin-list-section">
        <div class="admin-section-heading">
          <h2>Recent account notifications</h2>
          <span>{{ baseline.notificationEvents.length }} events</span>
        </div>
        <div class="admin-table" role="table" aria-label="Account notification events">
          @for (event of baseline.notificationEvents; track event.id) {
            <div class="admin-table__row">
              <span>
                <strong>{{ event.eventType }}</strong>
                <small>{{ event.recipientDisplayName }} · {{ event.recipientEmail }}</small>
              </span>
              <app-status-pill [label]="event.deliveryStatus" [tone]="event.deliveryStatus === 'SENT' ? 'ok' : 'warning'" />
            </div>
          } @empty {
            <app-empty-state title="No email events" body="User invitation and password reset events appear here." />
          }
        </div>
      </section>
    } @else {
      <app-empty-state title="Admin data unavailable" body="Sign in with an admin role and an active organization context." />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminManagementPageComponent {
  protected readonly adminService = inject(AdminManagementService);
  private readonly formBuilder = inject(FormBuilder);
  protected readonly baseline = signal<AdminManagementBaseline | null>(null);
  protected readonly saveMessage = signal<string | null>(null);
  protected readonly saveMessageTone = signal<'success' | 'error'>('success');
  protected readonly currentRole = computed(() => this.baseline()?.currentOrganization.role ?? 'USER');

  protected readonly userStatuses: UserLifecycleStatus[] = ['INVITED', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'];
  protected readonly membershipStatuses: UserLifecycleStatus[] = ['INVITED', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'];
  private readonly allRoles: UserRole[] = ['SUPERADMIN', 'ORG_ADMIN', 'USER'];

  protected readonly userForm = this.formBuilder.nonNullable.group({
    id: [''],
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', [Validators.required, Validators.maxLength(160)]],
    userStatus: ['INVITED'],
    role: ['USER'],
    membershipStatus: ['INVITED'],
    primaryMembership: [true],
  });

  constructor() {
    this.loadBaseline();
  }

  protected saveUser(baseline: AdminManagementBaseline): void {
    if (this.userForm.invalid || !this.canSubmitRole(this.userForm.controls.role.value, baseline)) {
      this.userForm.markAllAsTouched();
      this.saveMessageTone.set('error');
      this.saveMessage.set('Resolve validation errors before saving.');
      return;
    }

    const value = this.userForm.getRawValue();
    const createInput: AdminUserManagementInput = {
      email: value.email.trim(),
      displayName: value.displayName.trim(),
      userStatus: value.userStatus,
      role: value.role,
      membershipStatus: value.membershipStatus,
      primaryMembership: value.primaryMembership,
    };

    const request = value.id
      ? this.adminService.updateUser({ ...createInput, id: value.id } as UpdateAdminUserManagementInput)
      : this.adminService.createUser(createInput);

    request.subscribe((user) => {
      if (!user) {
        this.saveMessageTone.set('error');
        this.saveMessage.set('User save failed.');
        return;
      }
      this.saveMessageTone.set('success');
      this.saveMessage.set(value.id ? 'User updated.' : 'User invited.');
      this.resetUserForm();
      this.loadBaseline();
    });
  }

  protected editUser(user: AdminUserSummary): void {
    this.userForm.setValue({
      id: user.id,
      email: user.email,
      displayName: user.displayName,
      userStatus: user.status,
      role: user.role,
      membershipStatus: user.membershipStatus,
      primaryMembership: user.primaryMembership,
    });
    this.saveMessage.set(null);
  }

  protected resetUserForm(): void {
    this.userForm.reset({
      id: '',
      email: '',
      displayName: '',
      userStatus: 'INVITED',
      role: 'USER',
      membershipStatus: 'INVITED',
      primaryMembership: true,
    });
  }

  protected roleOptions(baseline: AdminManagementBaseline): UserRole[] {
    return baseline.currentOrganization.role === 'SUPERADMIN'
      ? this.allRoles
      : this.allRoles.filter((role) => role !== 'SUPERADMIN');
  }

  protected canSubmitRole(role: string, baseline: AdminManagementBaseline): boolean {
    return role !== 'SUPERADMIN' || baseline.currentOrganization.role === 'SUPERADMIN';
  }

  protected fieldError(control: AbstractControl, label: string): string | null {
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return `${label} is required.`;
    }
    if (control.hasError('email')) {
      return `${label} must be a valid email.`;
    }
    if (control.hasError('maxlength')) {
      return `${label} is too long.`;
    }
    return `${label} is invalid.`;
  }

  private loadBaseline(): void {
    this.adminService.fetchBaseline().subscribe((baseline) => this.baseline.set(baseline));
  }
}
