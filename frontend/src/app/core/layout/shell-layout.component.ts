import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { SidebarComponent } from './sidebar/sidebar.component';
import { TopbarComponent } from './topbar/topbar.component';

@Component({
  selector: 'app-shell-layout',
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="shell-frame">
      <app-sidebar />
      <div class="shell-main">
        <app-topbar />
        <main class="shell-content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellLayoutComponent {}

