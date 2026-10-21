import { Injectable } from '@angular/core';
import { AuthResponse, Role, Session } from '../types/user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // En local usamos la API del docker compose; en cualquier otro dominio (Vercel) la API desplegada.
  API_URL = ['localhost', '127.0.0.1'].includes(window.location.hostname)
    ? 'http://localhost:8080/api'
    : 'https://reservas-api-nt3e.onrender.com/api';
  TOKEN_KEY = 'auth_token';
  SESSION_KEY = 'session';

  async login(email: string, password: string): Promise<Session> {
    return this.autenticar('login', { email, password });
  }

  async register(email: string, password: string): Promise<Session> {
    return this.autenticar('register', { email, password });
  }

  // Login y registro hacen lo mismo con la respuesta, así que comparten método
  private async autenticar(accion: 'login' | 'register', body: { email: string; password: string }): Promise<Session> {
    const response = await fetch(`${this.API_URL}/auth/${accion}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      throw new Error(await this.leerError(response, 'No se pudo completar la operación'));
    }
    const data: AuthResponse = await response.json();
    localStorage.setItem(this.TOKEN_KEY, data.token);
    const session: Session = { email: data.email, role: data.role };
    localStorage.setItem(this.SESSION_KEY, JSON.stringify(session));
    return session;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.SESSION_KEY);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  getSession(): Session | null {
    try {
      const stored = localStorage.getItem(this.SESSION_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      localStorage.removeItem(this.SESSION_KEY);
      return null;
    }
  }

  getRole(): Role | null {
    return this.getSession()?.role ?? null;
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  // Petición autenticada genérica. Si el token ha caducado (401) cerramos sesión
  // y recargamos en /login. Así ningún componente tiene que repetir esa lógica.
  async request(path: string, options: RequestInit = {}): Promise<Response> {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const response = await fetch(`${this.API_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers,
      },
    });
    if (response.status === 401) {
      this.logout();
      window.location.href = '/login';
    }
    return response;
  }

  // El backend devuelve ProblemDetail (campo "detail"); por si acaso miramos también "message"
  async leerError(response: Response, porDefecto: string): Promise<string> {
    try {
      const data = await response.json();
      return data.detail || data.message || data.error || porDefecto;
    } catch {
      return porDefecto;
    }
  }
}
