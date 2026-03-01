import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditService } from '../../core/services/audit.service';
import {
  AuditEvent,
  AuditFilter,
  AuditAction,
  EventStatus,
  ResourceType,
  PageResponse,
} from '../../core/models/audit.model';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="audit-page">
      <div class="page-header">
        <h2>Audit Logs</h2>
        <button class="btn-export" (click)="exportCsv()">Export CSV</button>
      </div>

      <!-- Filters -->
      <div class="filters">
        <select [(ngModel)]="filter.action" (ngModelChange)="loadEvents()">
          <option [ngValue]="undefined">All Actions</option>
          <option *ngFor="let a of actions" [ngValue]="a">{{ a }}</option>
        </select>

        <select [(ngModel)]="filter.resourceType" (ngModelChange)="loadEvents()">
          <option [ngValue]="undefined">All Resources</option>
          <option *ngFor="let r of resourceTypes" [ngValue]="r">{{ r }}</option>
        </select>

        <select [(ngModel)]="filter.status" (ngModelChange)="loadEvents()">
          <option [ngValue]="undefined">All Statuses</option>
          <option *ngFor="let s of statuses" [ngValue]="s">{{ s }}</option>
        </select>

        <input
          type="text"
          placeholder="Search keyword..."
          [(ngModel)]="filter.keyword"
          (keyup.enter)="loadEvents()"
        />

        <input type="datetime-local" [(ngModel)]="filter.fromDate" (change)="loadEvents()" />
        <input type="datetime-local" [(ngModel)]="filter.toDate" (change)="loadEvents()" />
      </div>

      <!-- Table -->
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>Action</th>
              <th>Status</th>
              <th>Resource</th>
              <th>User ID</th>
              <th>IP Address</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of events">
              <td class="mono">{{ e.createdAt | date:'yyyy-MM-dd HH:mm:ss' }}</td>
              <td>
                <span class="badge" [class]="actionBadgeClass(e.action)">{{ e.action }}</span>
              </td>
              <td>
                <span class="status-dot" [class.success]="e.status === 'SUCCESS'" [class.failure]="e.status === 'FAILURE'"></span>
                {{ e.status }}
              </td>
              <td>{{ e.resourceType }}</td>
              <td class="mono truncate">{{ e.userId | slice:0:8 }}...</td>
              <td class="mono">{{ e.ipAddress }}</td>
              <td class="truncate">{{ e.description }}</td>
            </tr>
            <tr *ngIf="events.length === 0">
              <td colspan="7" class="empty">No audit events found.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination">
        <button [disabled]="filter.page === 0" (click)="changePage(filter.page - 1)">Previous</button>
        <span>Page {{ filter.page + 1 }} of {{ totalPages || 1 }} ({{ totalElements }} events)</span>
        <button [disabled]="filter.page >= totalPages - 1" (click)="changePage(filter.page + 1)">Next</button>
      </div>
    </div>
  `,
  styles: [`
    .audit-page { padding: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-header h2 { color: #f1f5f9; margin: 0; }
    .btn-export {
      padding: .5rem 1rem;
      background: #059669;
      color: #fff;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: .8rem;
      font-weight: 600;
    }
    .btn-export:hover { background: #047857; }

    .filters {
      display: flex;
      gap: .5rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
    }
    .filters select, .filters input {
      padding: .5rem .625rem;
      background: #1e293b;
      border: 1px solid #334155;
      color: #e2e8f0;
      border-radius: 6px;
      font-size: .8rem;
    }
    .filters select:focus, .filters input:focus { outline: none; border-color: #3b82f6; }

    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; font-size: .8rem; }
    th {
      text-align: left;
      padding: .625rem .5rem;
      color: #94a3b8;
      border-bottom: 1px solid #334155;
      white-space: nowrap;
      font-weight: 600;
    }
    td {
      padding: .5rem;
      color: #cbd5e1;
      border-bottom: 1px solid #1e293b;
    }
    .mono { font-family: 'JetBrains Mono', monospace; font-size: .75rem; }
    .truncate { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .empty { text-align: center; color: #64748b; padding: 2rem; }

    .badge {
      display: inline-block;
      padding: .15rem .5rem;
      border-radius: 4px;
      font-size: .7rem;
      font-weight: 600;
      background: #334155;
      color: #e2e8f0;
    }
    .badge.auth { background: #1e3a5f; color: #93c5fd; }
    .badge.secret { background: #3b1f54; color: #d8b4fe; }
    .badge.folder { background: #1a3a2a; color: #86efac; }
    .badge.share { background: #3b2f1a; color: #fcd34d; }

    .status-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 4px;
    }
    .status-dot.success { background: #22c55e; }
    .status-dot.failure { background: #ef4444; }

    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 1rem;
      margin-top: 1rem;
    }
    .pagination button {
      padding: .4rem .75rem;
      background: #334155;
      color: #e2e8f0;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: .8rem;
    }
    .pagination button:hover:not(:disabled) { background: #475569; }
    .pagination button:disabled { opacity: .4; cursor: not-allowed; }
    .pagination span { color: #94a3b8; font-size: .8rem; }
  `],
})
export class AuditComponent implements OnInit {
  events: AuditEvent[] = [];
  totalPages = 0;
  totalElements = 0;

  actions = Object.values(AuditAction);
  resourceTypes = Object.values(ResourceType);
  statuses = Object.values(EventStatus);

  filter: AuditFilter = { page: 0, size: 20 };

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.filter.page = 0;
    this.fetchEvents();
  }

  changePage(page: number): void {
    this.filter.page = page;
    this.fetchEvents();
  }

  private fetchEvents(): void {
    this.auditService.getEvents(this.filter).subscribe({
      next: (res: PageResponse<AuditEvent>) => {
        this.events = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
      },
      error: () => {
        this.events = [];
      },
    });
  }

  exportCsv(): void {
    this.auditService.exportCsv(this.filter).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'audit-events.csv';
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  actionBadgeClass(action: AuditAction): string {
    if (action.startsWith('USER_') || action.startsWith('TOKEN_') || action.startsWith('TOTP_')) return 'badge auth';
    if (action.startsWith('SECRET_') && !action.startsWith('SECRET_SHARED')) return 'badge secret';
    if (action.startsWith('FOLDER_')) return 'badge folder';
    return 'badge share';
  }
}
