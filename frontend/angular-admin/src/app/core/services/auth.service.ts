import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

interface LoginRequest {
  email: string;
  password: string;
}

interface AuthResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  message: string;
  accessToken: string;
  refreshToken: string;
  totpRequired: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'admin_token';
  private readonly USER_KEY = 'admin_user';
  private loggedIn$ = new BehaviorSubject<boolean>(this.hasToken());

  isLoggedIn$ = this.loggedIn$.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password } as LoginRequest)
      .pipe(
        tap((res) => {
          localStorage.setItem(this.TOKEN_KEY, res.accessToken);
          localStorage.setItem(this.USER_KEY, JSON.stringify({
            id: res.id,
            email: res.email,
            role: res.role,
            firstName: res.firstName,
            lastName: res.lastName,
          }));
          this.loggedIn$.next(true);
        })
      );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.loggedIn$.next(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUser(): { id: string; email: string; role: string } | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }
}
