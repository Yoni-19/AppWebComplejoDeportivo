import { EstadoCuenta, MetodoPago } from './pago.model';

/** Matches backend TipoDetalle enum (model/enums/TipoDetalle.java). */
export type TipoDetalle = 'CANCHA' | 'IMPLEMENTO' | 'DANIO' | 'PERDIDA' | 'SERVICIO';

/** Matches backend EstadoServicio enum (model/enums/EstadoServicio.java). */
export type EstadoServicio = 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'NO_ASISTIO';

export const ESTADOS_SERVICIO: EstadoServicio[] = [
  'CONFIRMADA',
  'EN_CURSO',
  'COMPLETADA',
  'CANCELADA',
  'NO_ASISTIO',
];

/** One line of the order-like breakdown (matches DetalleReservaResponse). */
export interface DetalleReserva {
  id: number | null;
  tipo: TipoDetalle;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

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
  cuentaId: number | null;
  estadoCuenta: EstadoCuenta | null;
  saldo: number | null;
  detalle: DetalleReserva[];
}

/**
 * Payload for POST/PUT /api/reservas (backend ReservaRequest DTO).
 * `estado` and `montoTotal` are absent on purpose: the backend computes the state
 * and the amount by hour started. `usuarioId` is optional: a CLIENTE books for
 * itself and the backend overwrites it. `metodoPago` is only used on create:
 * the backend registers the 50% deposit with that method.
 */
export interface ReservaAlta {
  fechaHoraInicio: string;
  fechaHoraFin: string;
  canchaId: number;
  usuarioId?: number;
  metodoPago?: MetodoPago;
}
