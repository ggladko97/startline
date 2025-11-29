export enum UserRole {
  CLIENT = 'CLIENT',
  APPRAISER = 'APPRAISER',
}

export enum OrderStatus {
  CREATED = 'CREATED',
  ASSIGNED = 'ASSIGNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export interface User {
  id: string;
  externalId: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export interface RegisterUserRequest {
  externalId: string;
  email: string;
  role?: UserRole;
}

export interface CreateOrderRequest {
  carMake: string;
  carModel: string;
  carYear: number;
  location: string;
  userId: string;
}

export interface Order {
  id: string;
  carMake: string;
  carModel: string;
  carYear: number;
  location: string;
  status: OrderStatus;
  userId: string;
  appraiserId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Report {
  id: string;
  orderId: string;
  description: string;
  fileUrl: string;
  createdAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp: string;
}
