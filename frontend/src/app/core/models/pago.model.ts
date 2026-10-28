/** Matches backend enums (model/enums/TipoPago, MetodoPago, EstadoPago, EstadoCuenta). */
export type TipoPago = 'ANTICIPO' | 'SALDO' | 'REEMBOLSO';
export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA';
export type EstadoPago = 'PENDIENTE' | 'PAGADO' | 'REEMBOLSADO';
export type EstadoCuenta = 'ABIERTA' | 'CERRADA' | 'ANULADA';

export const METODOS_PAGO: MetodoPago[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA'];

/** Matches PagoResponse: the account and reservation are flattened to ids. */
export interface Pago {
  id: number | null;
  monto: number;
  tipo: TipoPago;
  metodoPago: MetodoPago;
  estadoPago: EstadoPago;
  fechaPago: string;
  cuentaId: number;
  reservaId: number;
}

/** Matches CuentaResponse. */
export interface Cuenta {
  id: number;
  reservaId: number;
  total: number;
  saldo: number;
  estado: EstadoCuenta;
  pagos: Pago[];
}

/**
 * Payload for POST /api/pagos. The backend computes the amount for ANTICIPO
 * and SALDO; only REEMBOLSO needs an explicit `monto`.
 */
export interface PagoAlta {
  tipo: TipoPago;
  metodoPago: MetodoPago;
  reservaId: number;
  monto?: number;
}
