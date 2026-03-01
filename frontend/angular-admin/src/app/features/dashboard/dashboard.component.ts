import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuditService } from '../../core/services/audit.service';
import { AuditAction, EventStatus, ResourceType } from '../../core/models/audit.model';

interface StatCard {
  label: string;
  value: number;
  color: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dashboard">
      <h2>Dashboard</h2>

      <div class="section">
        <h3>Event Overview</h3>
        <div class="cards">
          <div class="card" *ngFor="let s of overviewStats" [style.border-left-color]="s.color">
            <span class="card-value">{{ s.value }}</span>
            <span class="card-label">{{ s.label }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3>By Action Type</h3>
        <div class="cards">
          <div class="card" *ngFor="let s of actionStats" [style.border-left-color]="s.color">
            <span class="card-value">{{ s.value }}</span>
            <span class="card-label">{{ s.label }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3>By Resource Type</h3>
        <div class="cards">
          <div class="card" *ngFor="let s of resourceStats" [style.border-left-color]="s.color">
            <span class="card-value">{{ s.value }}</span>
            <span class="card-label">{{ s.label }}</span>
          </div>
        </div>
      </div>

      <div class="section">
        <a routerLink="/audit" class="link">View All Audit Logs &rarr;</a>
      </div>
    </div>
  `,
  styles: [`
    .dashboard { padding: 1.5rem; }
    h2 { color: #f1f5f9; margin: 0 0 1.5rem; }
    h3 { color: #94a3b8; margin: 0 0 .75rem; font-size: .875rem; text-transform: uppercase; letter-spacing: .05em; }
    .section { margin-bottom: 2rem; }

    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: .75rem;
    }
    .card {
      background: #1e293b;
      border-radius: 8px;
      padding: 1rem;
      border-left: 3px solid #3b82f6;
    }
    .card-value {
      display: block;
      font-size: 1.75rem;
      font-weight: 700;
      color: #f1f5f9;
    }
    .card-label {
      display: block;
      font-size: .75rem;
      color: #94a3b8;
      margin-top: .25rem;
    }
    .link {
      color: #3b82f6;
      text-decoration: none;
      font-size: .875rem;
    }
    .link:hover { text-decoration: underline; }
  `],
})
export class DashboardComponent implements OnInit {
  overviewStats: StatCard[] = [];
  actionStats: StatCard[] = [];
  resourceStats: StatCard[] = [];

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  private loadStats(): void {
    const base = { page: 0, size: 1 };

    // Overview: total, success, failure
    const total$ = this.auditService.getEvents({ ...base });
    const success$ = this.auditService.getEvents({ ...base, status: EventStatus.SUCCESS });
    const failure$ = this.auditService.getEvents({ ...base, status: EventStatus.FAILURE });

    // By action: logins, login failures, secret reads, secret creates
    const logins$ = this.auditService.getEvents({ ...base, action: AuditAction.USER_LOGIN });
    const loginFails$ = this.auditService.getEvents({ ...base, action: AuditAction.USER_LOGIN_FAILED });
    const secretReads$ = this.auditService.getEvents({ ...base, action: AuditAction.SECRET_READ });
    const secretCreates$ = this.auditService.getEvents({ ...base, action: AuditAction.SECRET_CREATED });

    // By resource type
    const userEvents$ = this.auditService.getEvents({ ...base, resourceType: ResourceType.USER });
    const secretEvents$ = this.auditService.getEvents({ ...base, resourceType: ResourceType.SECRET });
    const folderEvents$ = this.auditService.getEvents({ ...base, resourceType: ResourceType.FOLDER });
    const shareEvents$ = this.auditService.getEvents({ ...base, resourceType: ResourceType.SHARE });

    forkJoin([
      total$, success$, failure$,
      logins$, loginFails$, secretReads$, secretCreates$,
      userEvents$, secretEvents$, folderEvents$, shareEvents$,
    ]).subscribe({
      next: ([total, success, failure, logins, loginFails, secretReads, secretCreates, userEv, secretEv, folderEv, shareEv]) => {
        this.overviewStats = [
          { label: 'Total Events', value: total.totalElements, color: '#3b82f6' },
          { label: 'Successful', value: success.totalElements, color: '#22c55e' },
          { label: 'Failed', value: failure.totalElements, color: '#ef4444' },
        ];

        this.actionStats = [
          { label: 'Logins', value: logins.totalElements, color: '#06b6d4' },
          { label: 'Failed Logins', value: loginFails.totalElements, color: '#f59e0b' },
          { label: 'Secrets Read', value: secretReads.totalElements, color: '#8b5cf6' },
          { label: 'Secrets Created', value: secretCreates.totalElements, color: '#a855f7' },
        ];

        this.resourceStats = [
          { label: 'User Events', value: userEv.totalElements, color: '#06b6d4' },
          { label: 'Secret Events', value: secretEv.totalElements, color: '#8b5cf6' },
          { label: 'Folder Events', value: folderEv.totalElements, color: '#22c55e' },
          { label: 'Share Events', value: shareEv.totalElements, color: '#f59e0b' },
        ];
      },
    });
  }
}
