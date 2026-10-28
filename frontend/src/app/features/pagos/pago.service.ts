import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import { Cuenta, Pago, PagoAlta } from '../../core/models/pago.model';

/**
 * Real HTTP service for the payment flow.
 * The payment amount is always computed by the backend; the client only
 * chooses the type and the method.
 */
@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/pagos`;

  list(): Observable<Pago[]> {
    return this.http.get<Pago[]>(this.url);
  }

  crear(pago: PagoAlta): Observable<Pago> {
    return this.http.post<Pago>(this.url, pago);
  }

  /** Account of a reservation, including its payments. */
  getCuenta(reservaId: number): Observable<Cuenta> {
    return this.http.get<Cuenta>(`${API_BASE}/api/reservas/${reservaId}/cuenta`);
  }
}
