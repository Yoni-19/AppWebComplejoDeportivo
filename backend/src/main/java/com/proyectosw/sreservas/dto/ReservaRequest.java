package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.proyectosw.sreservas.model.enums.MetodoPago;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Create/update payload for a reservation. References the court and the client by id
 * only; {@code estado} and {@code montoTotal} are computed by the service and therefore
 * excluded.
 *
 * {@code usuarioId} is optional in the payload: a CLIENTE books for itself and the
 * service overwrites whatever id arrives, while ADMIN and RECEPCIONISTA must send it.
 *
 * {@code metodoPago} is only used when creating: the service registers the 50% deposit
 * with that method. On update it is ignored, so the field stays optional.
 *
 * {@code implementos} son los implementos pedidos al reservar. Se usan solo al crear:
 * quedan como lineas del detalle sin costo y el {@code PUT} los ignora, porque el
 * prestamo ya esta registrado como movimiento.
 */
public record ReservaRequest(
        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "La fecha y hora de fin es obligatoria")
        LocalDateTime fechaHoraFin,

        @NotNull(message = "La reserva debe indicar una cancha")
        Integer canchaId,

        Integer usuarioId,

        MetodoPago metodoPago,

        List<@Valid ImplementoLineaRequest> implementos
) {
    /** Compatibilidad con las reservas que no piden implementos. */
    public ReservaRequest(LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
            Integer canchaId, Integer usuarioId, MetodoPago metodoPago) {
        this(fechaHoraInicio, fechaHoraFin, canchaId, usuarioId, metodoPago, List.of());
    }
}
