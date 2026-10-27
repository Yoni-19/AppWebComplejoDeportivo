/** Matches backend EstadoServicio enum (model/enums/EstadoServicio.java). */
export type EstadoServicio = 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'NO_ASISTIO';

export const ESTADOS_SERVICIO: EstadoServicio[] = [
  'CONFIRMADA',
  'EN_CURSO',
  'COMPLETADA',
  'CANCELADA',
  'NO_ASISTIO',
];

/** Matches backend ReservaResponse DTO (nested entities flattened to id + name). */
export interface Reserva {
  id: number | null;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  estado: EstadoServicio;
  montoTotal: number;
  canchaId: number | null;
  canchaNombre?: string;
  usuarioId: number | null;
  usuarioNombre?: string;
}

/**
 * Payload for POST/PUT /api/reservas (backend ReservaRequest DTO).
 * `estado` and `montoTotal` are absent on purpose: the backend computes the state
 * and the amount by hour started. `usuarioId` is optional: a CLIENTE books for
 * itself and the backend overwrites it.
 */
export interface ReservaAlta {
  fechaHoraInicio: string;
  fechaHoraFin: string;
  canchaId: number;
  usuarioId?: number;
}
