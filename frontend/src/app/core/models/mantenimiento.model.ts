/** Refleja el DTO backend MantenimientoResponse. */
export interface Mantenimiento {
  id: number | null;
  canchaId: number;
  canchaNombre: string;
  motivo: string;
  /** Trabajo realizado al resolver; null mientras el bloqueo siga abierto. */
  solucion: string | null;
  fechaHoraInicio: string;
  fechaHoraFin: string | null;
  resuelto: boolean;
  fechaCreacion: string;
  fechaResolucion: string | null;
  reservasAfectadas: number;
  reportadoPorNombre: string | null;
}

/** Payload de POST /api/mantenimientos (DTO backend MantenimientoRequest). */
export interface MantenimientoAlta {
  canchaId: number;
  motivo: string;
  /** Opcional: null o vacío significa "desde ahora". */
  fechaHoraInicio?: string | null;
}
