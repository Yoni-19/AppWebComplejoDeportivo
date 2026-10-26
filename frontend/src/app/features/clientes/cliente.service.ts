import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import { Cliente, ClienteAlta, ClienteEdicion, Rol } from '../../core/models/cliente.model';

/**
 * Real HTTP service — talks to the Spring Boot backend.
 * Method names mirror the REST endpoints (GET/POST/PUT/DELETE /api/usuarios).
 */
@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/usuarios`;

  list(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.url);
  }

  get(id: number): Observable<Cliente | undefined> {
    return this.http.get<Cliente>(`${this.url}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => (err.status === 404 ? of(undefined) : throwError(() => err))),
    );
  }

  create(cliente: ClienteAlta): Observable<Cliente> {
    return this.http.post<Cliente>(this.url, cliente);
  }

  update(cliente: Cliente, clave?: string): Observable<Cliente> {
    const payload: ClienteEdicion = {
      nombres: cliente.nombres,
      apellidos: cliente.apellidos,
      correo: cliente.correo,
    };
    if (clave) {
      payload.clave = clave;
    }
    return this.http.put<Cliente>(`${this.url}/${cliente.id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** PUT /api/usuarios/{id}/rol — only an ADMIN may change roles. */
  cambiarRol(id: number, rol: Rol): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.url}/${id}/rol`, { rol });
  }
}
