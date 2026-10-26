import { HttpErrorResponse } from '@angular/common/http';

/** Error envelope produced by the backend's GlobalExceptionHandler. */
export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  details?: Record<string, string>;
}

/**
 * Turns any failed HTTP call into a message that can be shown to the user.
 * Field-level validation errors win over the generic envelope message.
 */
export function apiErrorMessage(error: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor';
  }
  const body = error.error as ApiErrorBody | null;
  const detalles = body?.details ? Object.values(body.details) : [];
  if (detalles.length > 0) {
    return detalles.join(' · ');
  }
  return body?.message || body?.error || fallback;
}
