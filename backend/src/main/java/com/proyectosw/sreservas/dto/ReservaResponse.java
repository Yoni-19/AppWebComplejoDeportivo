package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoReserva;

/**
 * Reservation view. The nested {@code Usuario} and {@code Cancha} are
 * flattened to id + name to avoid serialization cycles and hash leakage.
 */
public record ReservaResponse(
        Integer id,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoReserva estadoReserva,
        BigDecimal montoTotal,
        Integer canchaId,
        String canchaNombre,
        Integer usuarioId,
        String usuarioNombre
) {
    public static ReservaResponse toResponse(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getFechaHoraInicio(),
                reserva.getFechaHoraFin(),
                reserva.getEstadoReserva(),
                reserva.getMontoTotal(),
                reserva.getCancha().getId(),
                reserva.getCancha().getNombre(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNombres() + " " + reserva.getUsuario().getApellidos()
        );
    }
}
