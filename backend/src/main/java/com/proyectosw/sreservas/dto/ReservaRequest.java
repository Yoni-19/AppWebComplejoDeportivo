package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/**
 * Create/update payload for a reservation. References the court and the client by id
 * only; {@code estado} and {@code montoTotal} are computed by the service and therefore
 * excluded.
 *
 * {@code usuarioId} is optional in the payload: a CLIENTE books for itself and the
 * service overwrites whatever id arrives, while ADMIN and RECEPCIONISTA must send it.
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
}
