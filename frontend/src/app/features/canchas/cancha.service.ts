import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import { Cancha } from '../../core/models/cancha.model';

/**
 * Real HTTP service — talks to the Spring Boot backend.
 * Method names mirror the REST endpoints (GET/POST/PUT/DELETE /api/canchas).
 */
@Injectable({ providedIn: 'root' })
export class CanchaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/canchas`;

  list(): Observable<Cancha[]> {
    return this.http.get<Cancha[]>(this.url);
  }

  get(id: number): Observable<Cancha | undefined> {
    return this.http.get<Cancha>(`${this.url}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => (err.status === 404 ? of(undefined) : throwError(() => err))),
    );
  }

  create(cancha: Omit<Cancha, 'id'>): Observable<Cancha> {
    return this.http.post<Cancha>(this.url, this.toRequest(cancha));
  }

  update(cancha: Cancha): Observable<Cancha> {
    return this.http.put<Cancha>(`${this.url}/${cancha.id}`, this.toRequest(cancha));
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** CanchaRequest = { nombre, deporte, precioPorHora, activa }; the id never travels. */
  private toRequest(cancha: Omit<Cancha, 'id'>): Omit<Cancha, 'id'> {
    const { nombre, deporte, precioPorHora, activa } = cancha;
    return { nombre, deporte, precioPorHora, activa };
  }
}
