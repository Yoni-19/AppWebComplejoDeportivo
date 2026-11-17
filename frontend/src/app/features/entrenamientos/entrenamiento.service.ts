import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { API_BASE } from '../../core/api.constants';
import {
  Entrenamiento,
  EntrenamientoAlta,
  InscripcionEntrenamiento,
} from '../../core/models/entrenamiento.model';

/** HTTP real contra /api/entrenamientos. */
@Injectable({ providedIn: 'root' })
export class EntrenamientoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/api/entrenamientos`;

  list(): Observable<Entrenamiento[]> {
    return this.http.get<Entrenamiento[]>(this.url);
  }

  get(id: number): Observable<Entrenamiento | undefined> {
    return this.http.get<Entrenamiento>(`${this.url}/${id}`).pipe(
      catchError((err: HttpErrorResponse) => (err.status === 404 ? of(undefined) : throwError(() => err))),
    );
  }

  create(entrenamiento: EntrenamientoAlta): Observable<Entrenamiento> {
    return this.http.post<Entrenamiento>(this.url, entrenamiento);
  }

  update(id: number, entrenamiento: EntrenamientoAlta): Observable<Entrenamiento> {
    return this.http.put<Entrenamiento>(`${this.url}/${id}`, entrenamiento);
  }

  cancelar(id: number): Observable<Entrenamiento> {
    return this.http.put<Entrenamiento>(`${this.url}/${id}/cancelar`, {});
  }

  listarInscripciones(id: number): Observable<InscripcionEntrenamiento[]> {
    return this.http.get<InscripcionEntrenamiento[]>(`${this.url}/${id}/inscripciones`);
  }

  /** El CLIENTE se inscribe a sí mismo; el backend ignora cualquier usuario del cuerpo. */
  inscribir(id: number): Observable<InscripcionEntrenamiento> {
    return this.http.post<InscripcionEntrenamiento>(`${this.url}/${id}/inscripciones`, {});
  }

  cancelarInscripcion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}/inscripciones`);
  }
}
