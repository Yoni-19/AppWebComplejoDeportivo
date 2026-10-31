package com.proyectosw.sreservas.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Implemento pedido al crear la reserva. Queda como linea IMPLEMENTO con precio
 * cero: el prestamo es gratuito y solo se cobra si se pierde o se dania.
 */
public record ImplementoLineaRequest(
        @NotNull(message = "La linea debe indicar el implemento")
        Integer implementoId,

        @NotNull(message = "La linea debe indicar la cantidad")
        @Min(value = 1, message = "La cantidad de implementos debe ser mayor a cero")
        Integer cantidad
) {
}
