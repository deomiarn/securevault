export enum AuditAction {
  USER_REGISTERED = 'USER_REGISTERED',
  USER_LOGIN = 'USER_LOGIN',
  USER_LOGIN_FAILED = 'USER_LOGIN_FAILED',
  USER_LOGOUT = 'USER_LOGOUT',
  TOKEN_REFRESHED = 'TOKEN_REFRESHED',
  TOTP_ENABLED = 'TOTP_ENABLED',
  TOTP_DISABLED = 'TOTP_DISABLED',
  TOTP_VERIFIED = 'TOTP_VERIFIED',
  SECRET_CREATED = 'SECRET_CREATED',
  SECRET_READ = 'SECRET_READ',
  SECRET_UPDATED = 'SECRET_UPDATED',
  SECRET_DELETED = 'SECRET_DELETED',
  FOLDER_CREATED = 'FOLDER_CREATED',
  FOLDER_UPDATED = 'FOLDER_UPDATED',
  FOLDER_DELETED = 'FOLDER_DELETED',
  SECRET_SHARED = 'SECRET_SHARED',
  SHARE_REVOKED = 'SHARE_REVOKED',
  SHARE_PERMISSION_CHANGED = 'SHARE_PERMISSION_CHANGED',
}

export enum EventStatus {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
}

export enum ResourceType {
  USER = 'USER',
  SECRET = 'SECRET',
  FOLDER = 'FOLDER',
  SHARE = 'SHARE',
}

export interface AuditEvent {
  id: string;
  userId: string;
  action: AuditAction;
  resourceType: ResourceType;
  resourceId: string;
  description: string;
  ipAddress: string;
  userAgent: string;
  status: EventStatus;
  metadata: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AuditFilter {
  userId?: string;
  action?: AuditAction;
  resourceType?: ResourceType;
  status?: EventStatus;
  fromDate?: string;
  toDate?: string;
  keyword?: string;
  page: number;
  size: number;
}
