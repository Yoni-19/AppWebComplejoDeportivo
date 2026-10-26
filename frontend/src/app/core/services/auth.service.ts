import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, of, switchMap, tap } from 'rxjs';
import { API_BASE } from '../api.constants';
import { UsuarioSesion } from '../models/cliente.model';

const TOKEN_KEY = 'auth_token';
const CORREO_KEY = 'auth_correo';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sesionState = signal<UsuarioSesion | null>(null);
  /** Current session, including the role. Null until it is loaded from the API. */
  readonly sesion = this.sesionState.asReadonly();

  readonly esAdmin = computed(() => this.sesionState()?.rol === 'ADMIN');
  readonly esStaff = computed(() => {
    const rol = this.sesionState()?.rol;
    return rol === 'ADMIN' || rol === 'RECEPCIONISTA';
  });
  readonly esCliente = computed(() => this.sesionState()?.rol === 'CLIENTE');

  /** POST /api/auth/login: stores the JWT and then loads the session (role included). */
  login(correo: string, clave: string): Observable<UsuarioSesion> {
    return this.http.post<{ token: string }>(`${API_BASE}/api/auth/login`, { correo, clave }).pipe(
      tap((res) => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(CORREO_KEY, correo);
      }),
      switchMap(() => this.cargarSesion()),
    );
  }

  /** GET /api/auth/me: the role always comes from the backend, never from localStorage. */
  cargarSesion(): Observable<UsuarioSesion> {
    return this.http.get<UsuarioSesion>(`${API_BASE}/api/auth/me`).pipe(
      tap((sesion) => this.sesionState.set(sesion)),
    );
  }

  /** Returns the cached session or fetches it once (used by guards and the layout). */
  asegurarSesion(): Observable<UsuarioSesion> {
    const actual = this.sesionState();
    return actual ? of(actual) : this.cargarSesion();
  }

  logout(): void {
    this.limpiar();
    this.router.navigate(['/login']);
  }

  /** Clears credentials without navigating (used by guards when the token is stale). */
  limpiar(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(CORREO_KEY);
    this.sesionState.set(null);
  }

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  get correo(): string | null {
    return localStorage.getItem(CORREO_KEY);
  }

  get isAuthenticated(): boolean {
    return !!this.token;
  }
}
