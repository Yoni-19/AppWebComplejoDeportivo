import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import { Reserva, ReservaAlta } from '../../core/models/reserva.model';

/**
 * Real HTTP service — talks to the Spring Boot backend.
 * The payload omits estado and montoTotal: the backend computes both.
 */
@Injectable({ providedIn: 'root' })
export class ReservaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/reservas`;

  list(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(this.url);
  }

  get(id: number): Observable<Reserva | undefined> {
    return this.http.get<Reserva>(`${this.url}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => (err.status === 404 ? of(undefined) : throwError(() => err))),
    );
  }

  create(reserva: ReservaAlta): Observable<Reserva> {
    return this.http.post<Reserva>(this.url, reserva);
  }

  update(id: number, reserva: ReservaAlta): Observable<Reserva> {
    return this.http.put<Reserva>(`${this.url}/${id}`, reserva);
  }

  /** Transicion de negocio: conserva la reserva y libera el turno. */
  cancelar(id: number): Observable<Reserva> {
    return this.http.put<Reserva>(`${this.url}/${id}/cancelar`, {});
  }

  /** Borrado fisico; el backend solo lo permite a un ADMIN. */
  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
