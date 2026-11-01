/**
 * Catálogo de implementos del complejo: pelotas, redes, petos.
 * No hay inventario en el sistema; solo qué se puede prestar y cuánto cuesta
 * reponer una unidad perdida o dañada.
 */
export interface Implemento {
  id: number | null;
  nombre: string;
  valorReposicion: number;
  activo: boolean;
}

/** POST/PUT /api/implementos (backend ImplementoRequest). */
export interface ImplementoAlta {
  nombre: string;
  valorReposicion: number;
  activo: boolean;
}

/** Un implemento pedido al reservar (backend ImplementoLineaRequest). */
export interface ImplementoLineaAlta {
  implementoId: number;
  cantidad: number;
}

/** Unidad entregada por utilería (backend EntregaImplementoRequest). */
export interface EntregaImplemento {
  implementoId: number;
  cantidad: number;
}

/**
 * Devolución de una unidad. `devueltas` incluye las dañadas: volvieron pero se
 * cobran. `danadas` es un subconjunto de `devueltas`; las unidades faltantes
 * (`entregados - devueltas`) se cobran como pérdida. Si no se indica precio, el
 * backend usa el valor de reposición del catálogo.
 */
export interface DevolucionImplemento {
  implementoId: number;
  devueltas: number;
  danadas?: number;
  precioDanio?: number | null;
  precioPerdida?: number | null;
}

export type TipoCargo = 'DANIO' | 'PERDIDA' | 'SERVICIO';

export const TIPOS_CARGO: TipoCargo[] = ['DANIO', 'PERDIDA', 'SERVICIO'];

/** Cargo manual que agrega utilería (backend CargoRequest). */
export interface CargoAlta {
  tipo: TipoCargo;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
}

/** GET /api/reservas/{id}/implementos (backend ImplementoReservaResponse). */
export interface ImplementoReserva {
  reservaId: number;
  lineas: LineaImplementoReserva[];
}

export interface LineaImplementoReserva {
  implementoId: number;
  nombre: string;
  pedidos: number;
  entregados: number;
  devueltos: number;
  pendientes: number;
  /** Precio que usa el backend para cobrar una unidad dañada o perdida. */
  valorReposicion: number;
}
