package com.proyectosw.sreservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cierre de un mantenimiento.
 *
 * {@code solucion} describe el trabajo realizado para liberar la cancha y queda
 * en el historial para que el admin pueda consultarlo.
 */
public record MantenimientoResolucionRequest(
        @NotBlank(message = "La solución es obligatoria")
        @Size(max = 500, message = "La solución no puede superar los 500 caracteres")
        String solucion
) {
}
