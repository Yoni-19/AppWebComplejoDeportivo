import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE } from '../api.constants';
import { Notificacion } from '../models/notificacion.model';

/** Bandeja de avisos del usuario autenticado y contador de la campana. */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/notificaciones`;

  /** Badge del layout: se actualiza al listar o marcar avisos. */
  readonly noLeidas = signal(0);

  list(): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(this.url).pipe(
      tap((notificaciones) => {
        this.noLeidas.set(notificaciones.filter((n) => !n.leida).length);
      }),
    );
  }

  contarNoLeidas(): Observable<{ noLeidas: number }> {
    return this.http.get<{ noLeidas: number }>(`${this.url}/no-leidas/count`).pipe(
      tap((res) => this.noLeidas.set(res.noLeidas)),
    );
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.put<Notificacion>(`${this.url}/${id}/leer`, {}).pipe(
      tap((notificacion) => {
        if (notificacion.leida) {
          this.noLeidas.update((count) => Math.max(count - 1, 0));
        }
      }),
    );
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.put<void>(`${this.url}/leer-todas`, {}).pipe(
      tap(() => this.noLeidas.set(0)),
    );
  }
}
