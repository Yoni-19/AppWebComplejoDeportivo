/** Refleja el enum backend TipoNotificacion. */
export type TipoNotificacion =
  | 'ENTRENAMIENTO_CANCELADO'
  | 'ENTRENAMIENTO_REPROGRAMADO'
  | 'MANTENIMIENTO_ABIERTO'
  | 'MANTENIMIENTO_RESUELTO';

/** Refleja el DTO backend NotificacionResponse. */
export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje: string;
  leida: boolean;
  fechaCreacion: string;
  referencia: string | null;
}
