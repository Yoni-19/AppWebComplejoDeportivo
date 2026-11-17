/** Refleja el enum backend EstadoEntrenamiento. */
export type EstadoEntrenamiento = 'PROGRAMADO' | 'CANCELADO';

/** Refleja el enum backend EstadoInscripcion. */
export type EstadoInscripcion = 'INSCRITO' | 'CANCELADO';

/** Refleja el DTO backend EntrenamientoResponse. */
export interface Entrenamiento {
  id: number | null;
  titulo: string;
  entrenador: string;
  descripcion: string | null;
  precio: number;
  cupoMaximo: number;
  inscriptos: number;
  cupoDisponible: number;
  estado: EstadoEntrenamiento;
  canchaId: number;
  canchaNombre: string;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  /** True cuando el usuario autenticado ocupa un cupo. */
  inscripto: boolean;
}

/** Payload de POST/PUT /api/entrenamientos (DTO backend EntrenamientoRequest). */
export interface EntrenamientoAlta {
  canchaId: number;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  titulo: string;
  entrenador: string;
  descripcion?: string;
  cupoMaximo: number;
  precio: number;
}

/** Refleja el DTO backend InscripcionEntrenamientoResponse. */
export interface InscripcionEntrenamiento {
  id: number | null;
  entrenamientoId: number;
  usuarioId: number;
  usuarioNombre: string;
  estado: EstadoInscripcion;
  fechaInscripcion: string;
}
