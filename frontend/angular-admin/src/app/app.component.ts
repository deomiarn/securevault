import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-layout" *ngIf="auth.isLoggedIn$ | async; else noNav">
      <nav class="sidebar">
        <div class="logo">SecureVault</div>
        <div class="nav-label">ADMIN</div>
        <a routerLink="/dashboard" routerLinkActive="active">Dashboard</a>
        <a routerLink="/audit" routerLinkActive="active">Audit Logs</a>
        <div class="spacer"></div>
        <button class="logout-btn" (click)="auth.logout()">Logout</button>
      </nav>
      <main class="content">
        <router-outlet />
      </main>
    </div>
    <ng-template #noNav>
      <router-outlet />
    </ng-template>
  `,
  styles: [`
    .app-layout { display: flex; min-height: 100vh; }
    .sidebar {
      width: 220px;
      background: #1e293b;
      padding: 1.25rem 1rem;
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
    }
    .logo {
      font-size: 1.125rem;
      font-weight: 700;
      color: #f1f5f9;
      margin-bottom: .25rem;
    }
    .nav-label {
      font-size: .65rem;
      color: #64748b;
      letter-spacing: .1em;
      margin-bottom: 1.5rem;
    }
    .sidebar a {
      display: block;
      padding: .5rem .75rem;
      color: #94a3b8;
      text-decoration: none;
      border-radius: 6px;
      font-size: .875rem;
      margin-bottom: .25rem;
    }
    .sidebar a:hover { background: #334155; color: #e2e8f0; }
    .sidebar a.active { background: #3b82f6; color: #fff; }
    .spacer { flex: 1; }
    .logout-btn {
      padding: .5rem .75rem;
      background: transparent;
      border: 1px solid #475569;
      color: #94a3b8;
      border-radius: 6px;
      cursor: pointer;
      font-size: .8rem;
    }
    .logout-btn:hover { background: #334155; color: #e2e8f0; }
    .content { flex: 1; background: #0f172a; overflow-y: auto; }
  `],
})
export class AppComponent {
  constructor(public auth: AuthService) {}
}
