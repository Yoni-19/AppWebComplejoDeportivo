package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Notificacion;
import com.proyectosw.sreservas.model.enums.TipoNotificacion;

/** Aviso de la campana del layout. */
public record NotificacionResponse(
        Integer id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime fechaCreacion,
        String referencia
) {
    public static NotificacionResponse toResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion(),
                notificacion.getReferencia()
        );
    }
}
