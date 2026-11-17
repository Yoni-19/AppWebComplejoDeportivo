import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import { Mantenimiento, MantenimientoAlta } from '../../core/models/mantenimiento.model';

/** HTTP real contra /api/mantenimientos. */
@Injectable({ providedIn: 'root' })
export class MantenimientoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/mantenimientos`;

  list(): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(this.url);
  }

  abrir(mantenimiento: MantenimientoAlta): Observable<Mantenimiento> {
    return this.http.post<Mantenimiento>(this.url, mantenimiento);
  }

  resolver(id: number, solucion: string): Observable<Mantenimiento> {
    return this.http.put<Mantenimiento>(`${this.url}/${id}/resolver`, { solucion });
  }
}
