import axios, { AxiosInstance, AxiosError } from 'axios';
import {
  User,
  RegisterUserRequest,
  CreateOrderRequest,
  Order,
  Report,
  PaginatedResponse,
} from '../types/api';

const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1';

class ApiService {
  private client: AxiosInstance;
  private authToken: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    this.client.interceptors.request.use(
      (config) => {
        if (this.authToken) {
          config.headers.Authorization = `Bearer ${this.authToken}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.code === 'ECONNABORTED' || error.message === 'Network Error') {
          console.error('Backend is not available');
        }
        return Promise.reject(error);
      }
    );
  }

  setAuthToken(token: string) {
    this.authToken = token;
  }

  clearAuthToken() {
    this.authToken = null;
  }

  async registerUser(data: RegisterUserRequest): Promise<User> {
    const response = await this.client.post<User>('/users/register', data);
    return response.data;
  }

  async getUser(externalId: string): Promise<User> {
    const response = await this.client.get<User>(`/users/${externalId}`);
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get<User>('/users/me');
    return response.data;
  }

  async createOrder(data: CreateOrderRequest): Promise<Order> {
    const response = await this.client.post<Order>('/orders', data);
    return response.data;
  }

  async getOrders(
    userId?: string,
    appraiserId?: string,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<Order>> {
    const params: any = { page, size };
    if (userId) params.userId = userId;
    if (appraiserId) params.appraiserId = appraiserId;

    const response = await this.client.get<PaginatedResponse<Order>>('/orders', {
      params,
    });
    return response.data;
  }

  async getOrder(orderId: string): Promise<Order> {
    const response = await this.client.get<Order>(`/orders/${orderId}`);
    return response.data;
  }

  async assignOrder(orderId: string, appraiserId: string): Promise<Order> {
    const response = await this.client.post<Order>(
      `/orders/${orderId}/assign`,
      { appraiserId }
    );
    return response.data;
  }

  async updateOrderStatus(orderId: string, status: string): Promise<Order> {
    const response = await this.client.put<Order>(`/orders/${orderId}/status`, {
      status,
    });
    return response.data;
  }

  async uploadReport(
    orderId: string,
    file: File | Blob,
    description: string
  ): Promise<Report> {
    const formData = new FormData();
    formData.append('file', file, `report+${orderId}.pdf`);
    formData.append('description', description);

    const response = await this.client.post<Report>(
      `/reports/orders/${orderId}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  }

  async getReport(orderId: string): Promise<Report> {
    const response = await this.client.get<Report>(`/reports/orders/${orderId}`);
    return response.data;
  }

  async getAppraiserWhitelist(): Promise<string[]> {
    const response = await this.client.get<string[]>('/users/appraisers');
    return response.data;
  }
}

export const apiService = new ApiService();
