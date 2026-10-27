package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoServicio;

/**
 * Reservation view. The court and the time range are read from the reservation's
 * {@link Ocupacion}; the nested {@code Usuario} and {@code Cancha} are flattened to
 * id + name to avoid serialization cycles and hash leakage.
 */
public record ReservaResponse(
        Integer id,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoServicio estado,
        BigDecimal montoTotal,
        Integer canchaId,
        String canchaNombre,
        Integer usuarioId,
        String usuarioNombre
) {
    public static ReservaResponse toResponse(Reserva reserva) {
        Ocupacion ocupacion = reserva.getOcupacion();
        return new ReservaResponse(
                reserva.getId(),
                ocupacion.getFechaHoraInicio(),
                ocupacion.getFechaHoraFin(),
                reserva.getEstado(),
                reserva.getMontoTotal(),
                ocupacion.getCancha().getId(),
                ocupacion.getCancha().getNombre(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNombres() + " " + reserva.getUsuario().getApellidos()
        );
    }
}
