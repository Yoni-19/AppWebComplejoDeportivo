/** Matches backend TipoDeporte enum (model/enums/TipoDeporte.java). */
export type TipoDeporte = 'FUTBOL' | 'FUTSAL' | 'VOLEY' | 'BASQUET' | 'TENIS';

export const TIPOS_DEPORTE: TipoDeporte[] = ['FUTBOL', 'FUTSAL', 'VOLEY', 'BASQUET', 'TENIS'];

/** Matches backend CanchaResponse DTO. */
export interface Cancha {
  id: number | null;
  nombre: string;
  deporte: TipoDeporte;
  precioPorHora: number;
  activa: boolean;
}
