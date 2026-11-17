package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Apertura de un mantenimiento.
 *
 * {@code fechaHoraInicio} es opcional: nula significa "desde ahora". La fecha
 * de fin no se recibe porque el bloqueo queda abierto hasta resolverlo.
 */
public record MantenimientoRequest(
        @NotNull(message = "La cancha es obligatoria")
        Integer canchaId,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
        String motivo,

        LocalDateTime fechaHoraInicio
) {
}
