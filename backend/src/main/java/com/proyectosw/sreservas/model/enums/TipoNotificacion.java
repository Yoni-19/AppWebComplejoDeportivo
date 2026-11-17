package com.proyectosw.sreservas.model.enums;

/**
 * Motivo de una notificacion interna del sistema.
 *
 * El tipo permite que el frontend elija icono y color sin parsear el mensaje.
 */
public enum TipoNotificacion {
    ENTRENAMIENTO_CANCELADO,
    ENTRENAMIENTO_REPROGRAMADO,
    MANTENIMIENTO_ABIERTO,
    MANTENIMIENTO_RESUELTO
}
