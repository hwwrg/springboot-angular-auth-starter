import { Bell, LayoutDashboard, LucideIconData, Settings, ShieldCheck, Users } from 'lucide-angular';

import { AuthRole } from '../auth/auth.model';

export interface ShellNavigationItem {
  labelKey:
    | 'nav.dashboard'
    | 'nav.workspace'
    | 'nav.notifications'
    | 'nav.account'
    | 'nav.admin';
  route: string;
  icon: LucideIconData;
  placeholder?: boolean;
  allowedRoles?: AuthRole[];
}

export const PRIMARY_NAVIGATION: ShellNavigationItem[] = [
  { labelKey: 'nav.dashboard', route: '/app/dashboard', icon: LayoutDashboard },
  { labelKey: 'nav.workspace', route: '/app/workspace', icon: Users },
  { labelKey: 'nav.notifications', route: '/app/notifications', icon: Bell },
];

export const SECONDARY_NAVIGATION: ShellNavigationItem[] = [
  {
    labelKey: 'nav.account',
    route: '/app/account',
    icon: ShieldCheck,
  },
  {
    labelKey: 'nav.admin',
    route: '/app/admin',
    icon: Settings,
    allowedRoles: ['SUPERADMIN', 'ORG_ADMIN'],
  },
];
