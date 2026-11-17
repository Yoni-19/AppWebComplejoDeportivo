package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta y reprogramacion de un entrenamiento.
 *
 * La cancha y el horario se guardan en la ocupacion; el resto son datos propios
 * del entrenamiento. El estado no se recibe: lo maneja el backend.
 */
public record EntrenamientoRequest(
        @NotNull(message = "La cancha es obligatoria")
        Integer canchaId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDateTime fechaHoraInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDateTime fechaHoraFin,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 120, message = "El titulo no puede superar los 120 caracteres")
        String titulo,

        @NotBlank(message = "El entrenador es obligatorio")
        @Size(max = 100, message = "El entrenador no puede superar los 100 caracteres")
        String entrenador,

        @Size(max = 300, message = "La descripcion no puede superar los 300 caracteres")
        String descripcion,

        @NotNull(message = "El cupo es obligatorio")
        @Min(value = 1, message = "El cupo debe ser al menos 1")
        Integer cupoMaximo,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        BigDecimal precio
) {
}
