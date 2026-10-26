package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;

import jakarta.validation.constraints.NotNull;

/**
 * Create/update payload for a reservation. References {@code usuario} and
 * {@code cancha} by id only; {@code estadoReserva} and {@code montoTotal}
 * are computed by the service and therefore excluded.
 *
 * {@code usuarioId} is optional in the payload: a CLIENTE books for itself and
 * the service overwrites whatever id arrives, while ADMIN and RECEPCIONISTA
 * must send it.
 */
public record ReservaRequest(
        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "La fecha y hora de fin es obligatoria")
        LocalDateTime fechaHoraFin,

        @NotNull(message = "La reserva debe indicar una cancha")
        Integer canchaId,

        Integer usuarioId
) {
    public Reserva toEntity() {
        Reserva reserva = new Reserva();
        reserva.setFechaHoraInicio(fechaHoraInicio);
        reserva.setFechaHoraFin(fechaHoraFin);

        Cancha cancha = new Cancha();
        cancha.setId(canchaId);
        reserva.setCancha(cancha);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        reserva.setUsuario(usuario);

        return reserva;
    }
}
