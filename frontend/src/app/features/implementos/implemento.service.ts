import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import {
  CargoAlta,
  DevolucionImplemento,
  EntregaImplemento,
  Implemento,
  ImplementoAlta,
  ImplementoReserva,
} from '../../core/models/implemento.model';
import { Reserva } from '../../core/models/reserva.model';

/**
 * Servicio HTTP real del catálogo de implementos y del proceso de utilería.
 * El backend recalcula el total de la reserva después de cada cargo, así la
 * caja siempre cobra lo que corresponde.
 */
@Injectable({ providedIn: 'root' })
export class ImplementoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/implementos`;
  private readonly reservasUrl = `${API_BASE}/api/reservas`;

  list(): Observable<Implemento[]> {
    return this.http.get<Implemento[]>(this.url);
  }

  create(implemento: ImplementoAlta): Observable<Implemento> {
    return this.http.post<Implemento>(this.url, implemento);
  }

  update(id: number, implemento: ImplementoAlta): Observable<Implemento> {
    return this.http.put<Implemento>(`${this.url}/${id}`, implemento);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Unidades pedidas, entregadas, devueltas y pendientes de una reserva. */
  estadoImplementos(reservaId: number): Observable<ImplementoReserva> {
    return this.http.get<ImplementoReserva>(`${this.reservasUrl}/${reservaId}/implementos`);
  }

  entregar(reservaId: number, lineas: EntregaImplemento[]): Observable<ImplementoReserva> {
    return this.http.post<ImplementoReserva>(`${this.reservasUrl}/${reservaId}/implementos/entrega`, lineas);
  }

  /** La respuesta es la reserva con el total recalculado. */
  devolver(reservaId: number, lineas: DevolucionImplemento[]): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.reservasUrl}/${reservaId}/implementos/devolucion`, lineas);
  }

  agregarCargo(reservaId: number, cargo: CargoAlta): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.reservasUrl}/${reservaId}/cargos`, cargo);
  }
}
