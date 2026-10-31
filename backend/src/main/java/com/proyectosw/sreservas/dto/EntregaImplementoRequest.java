package com.proyectosw.sreservas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Una linea de la entrega de implementos que registra utileria. */
public record EntregaImplementoRequest(
        @NotNull(message = "La entrega debe indicar el implemento")
        Integer implementoId,

        @NotNull(message = "La entrega debe indicar la cantidad")
        @Min(value = 1, message = "La cantidad entregada debe ser mayor a cero")
        Integer cantidad
) {
}
