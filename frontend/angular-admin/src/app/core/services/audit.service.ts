import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditEvent, AuditFilter, PageResponse } from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly baseUrl = '/api/audit';

  constructor(private http: HttpClient) {}

  getEvents(filter: AuditFilter): Observable<PageResponse<AuditEvent>> {
    let params = new HttpParams()
      .set('page', filter.page.toString())
      .set('size', filter.size.toString());

    if (filter.userId) params = params.set('userId', filter.userId);
    if (filter.action) params = params.set('action', filter.action);
    if (filter.resourceType) params = params.set('resourceType', filter.resourceType);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter.toDate) params = params.set('toDate', filter.toDate);
    if (filter.keyword) params = params.set('keyword', filter.keyword);

    return this.http.get<PageResponse<AuditEvent>>(`${this.baseUrl}/events`, { params });
  }

  exportCsv(filter: Partial<AuditFilter>): Observable<Blob> {
    let params = new HttpParams();
    if (filter.action) params = params.set('action', filter.action);
    if (filter.resourceType) params = params.set('resourceType', filter.resourceType);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter.toDate) params = params.set('toDate', filter.toDate);

    return this.http.get(`${this.baseUrl}/events/export/csv`, {
      params,
      responseType: 'blob',
    });
  }
}
