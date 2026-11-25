/** Transport scaffold for React Native / other TypeScript mobile clients.
 * Implement TokenStore with Keychain / Keystore, never AsyncStorage.
 */
export interface TokenStore {
  get(): Promise<string | null>;
  set(refreshToken: string): Promise<void>;
  clear(): Promise<void>;
}
export type Role = 'VIEWER' | 'EDITOR' | 'ADMIN';
export interface User { id: string; login: string; name: string; roles: Role[]; active: boolean; mustChangePassword: boolean; version: number }
export interface Tokens { accessToken: string; refreshToken: string; expiresIn: number; user: User }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface Customer {
  id: string; code: string; name: string; email: string | null; phone: string | null;
  description: string | null; segmentId: string; segmentName: string;
  status: 'ACTIVE' | 'BLOCKED' | 'INACTIVE'; totalPurchases: number;
  marketingConsent: boolean; tags: string[]; tier: 'BASE' | 'SILVER' | 'GOLD';
  createdAt: string; updatedAt: string; version: number; archived: boolean;
}
export type CustomerInput = Omit<Customer, 'id' | 'segmentName' | 'tier' | 'createdAt' | 'updatedAt' | 'archived'>;
export interface ApiProblem { timestamp: string; status: number; message: string; correlationId: string; fields: Record<string,string> }
export class ApiError extends Error { constructor(public problem: ApiProblem) { super(problem.message); } }
export class LoyaltyApi {
  private accessToken = '';
  private refreshing: Promise<Tokens> | null = null;
  constructor(private baseUrl: string, private store: TokenStore) {}
  private async send<T>(path: string, method = 'GET', body?: unknown, authenticated = true): Promise<T> {
    const response = await fetch(this.baseUrl.replace(/\/$/, '') + '/api' + path, {
      method, headers: { 'Content-Type': 'application/json', 'X-Refresh-Request': '1',
        ...(authenticated && this.accessToken ? { Authorization: `Bearer ${this.accessToken}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) throw new ApiError(await response.json() as ApiProblem);
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
  private async accept(tokens: Tokens): Promise<Tokens> {
    await this.store.set(tokens.refreshToken);
    this.accessToken = tokens.accessToken;
    return tokens;
  }
  async login(login: string, password: string): Promise<Tokens> {
    return this.accept(await this.send<Tokens>('/auth/login', 'POST', { login, password, mobile: true }, false));
  }
  async refresh(): Promise<Tokens> {
    if (!this.refreshing) this.refreshing = (async () => {
      const refreshToken = await this.store.get();
      if (!refreshToken) throw new Error('Sign-in required');
      try { return await this.accept(await this.send<Tokens>('/auth/refresh', 'POST', { refreshToken }, false)); }
      catch (error) {
        if (error instanceof ApiError && error.problem.status === 401) { this.accessToken = ''; await this.store.clear(); }
        throw error;
      }
    })().finally(() => { this.refreshing = null; });
    return this.refreshing;
  }
  async request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
    if (!this.accessToken) await this.refresh();
    try { return await this.send<T>(path, method, body); }
    catch (error) {
      if (!(error instanceof ApiError) || error.problem.status !== 401) throw error;
      await this.refresh();
      return this.send<T>(path, method, body);
    }
  }
  me() { return this.request<User>('/auth/me'); }
  customers(filters: Record<string,string> = {}) { return this.request<Page<Customer>>('/customers?' + new URLSearchParams(filters)); }
  customer(id: string) { return this.request<Customer>('/customers/' + encodeURIComponent(id)); }
  createCustomer(input: CustomerInput) { return this.request<Customer>('/customers', 'POST', input); }
  updateCustomer(id: string, input: CustomerInput) { return this.request<Customer>('/customers/' + encodeURIComponent(id), 'PUT', input); }
  async changePassword(currentPassword: string, newPassword: string) {
    await this.request('/auth/password', 'POST', { currentPassword, newPassword });
    this.accessToken = ''; await this.store.clear();
  }
  async logout() {
    const refreshToken = await this.store.get();
    // Retain the token if the network fails, so a retry can revoke the server session.
    if (refreshToken) await this.send('/auth/logout', 'POST', { refreshToken }, false);
    this.accessToken = ''; await this.store.clear();
  }
}
