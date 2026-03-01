import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-wrapper">
      <form class="login-card" (ngSubmit)="onLogin()">
        <h1>SecureVault Admin</h1>
        <p class="subtitle">Audit Dashboard Login</p>

        <div *ngIf="error" class="error-msg">{{ error }}</div>

        <label for="email">Email</label>
        <input
          id="email"
          type="email"
          [(ngModel)]="email"
          name="email"
          placeholder="admin@securevault.io"
          required
        />

        <label for="password">Password</label>
        <input
          id="password"
          type="password"
          [(ngModel)]="password"
          name="password"
          placeholder="Password"
          required
        />

        <button type="submit" [disabled]="loading">
          {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>
    </div>
  `,
  styles: [`
    .login-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: #0f172a;
    }
    .login-card {
      background: #1e293b;
      padding: 2.5rem;
      border-radius: 12px;
      width: 380px;
      box-shadow: 0 4px 24px rgba(0,0,0,.4);
    }
    h1 { color: #f1f5f9; margin: 0 0 .25rem; font-size: 1.5rem; }
    .subtitle { color: #94a3b8; margin: 0 0 1.5rem; font-size: .875rem; }
    label { display: block; color: #cbd5e1; margin-bottom: .25rem; font-size: .875rem; }
    input {
      width: 100%;
      padding: .625rem .75rem;
      margin-bottom: 1rem;
      border: 1px solid #334155;
      border-radius: 6px;
      background: #0f172a;
      color: #f1f5f9;
      font-size: .875rem;
      box-sizing: border-box;
    }
    input:focus { outline: none; border-color: #3b82f6; }
    button {
      width: 100%;
      padding: .625rem;
      background: #3b82f6;
      color: #fff;
      border: none;
      border-radius: 6px;
      font-size: .875rem;
      cursor: pointer;
      font-weight: 600;
    }
    button:hover:not(:disabled) { background: #2563eb; }
    button:disabled { opacity: .6; cursor: not-allowed; }
    .error-msg {
      background: #7f1d1d;
      color: #fca5a5;
      padding: .5rem .75rem;
      border-radius: 6px;
      margin-bottom: 1rem;
      font-size: .8rem;
    }
  `],
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onLogin(): void {
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Login failed. Check your credentials.';
      },
    });
  }
}
