import { ImplementoLineaAlta } from './implemento.model';
import { EstadoCuenta, MetodoPago } from './pago.model';

/** Refleja el enum backend TipoDetalle (model/enums/TipoDetalle.java). */
export type TipoDetalle = 'CANCHA' | 'IMPLEMENTO' | 'DANIO' | 'PERDIDA' | 'SERVICIO';

/** Refleja el enum backend EstadoServicio (model/enums/EstadoServicio.java). */
export type EstadoServicio = 'CONFIRMADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'NO_ASISTIO';

export const ESTADOS_SERVICIO: EstadoServicio[] = [
  'CONFIRMADA',
  'EN_CURSO',
  'COMPLETADA',
  'CANCELADA',
  'NO_ASISTIO',
];

/** Una línea del detalle tipo pedido (refleja DetalleReservaResponse). */
export interface DetalleReserva {
  id: number | null;
  tipo: TipoDetalle;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  /** Solo en líneas IMPLEMENTO: qué implemento del catálogo se pidió. */
  implementoId?: number | null;
}

/** Refleja el DTO backend ReservaResponse (entidades anidadas aplanadas a id + nombre). */
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
 * Payload de POST/PUT /api/reservas (DTO backend ReservaRequest).
 * `estado` y `montoTotal` no van a propósito: el backend calcula el estado y el
 * monto por hora iniciada. `usuarioId` es opcional: un CLIENTE reserva para sí
 * mismo y el backend lo sobrescribe. `metodoPago` se usa solo al crear: el
 * backend registra el anticipo del 50% con ese método. Los `implementos`
 * también se usan solo al crear: quedan como líneas sin costo que solo se cobran
 * si se dañan o se pierden.
 */
export interface ReservaAlta {
  fechaHoraInicio: string;
  fechaHoraFin: string;
  canchaId: number;
  usuarioId?: number;
  metodoPago?: MetodoPago;
  implementos?: ImplementoLineaAlta[];
}
