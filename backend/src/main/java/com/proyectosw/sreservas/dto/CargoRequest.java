package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.enums.TipoDetalle;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cargo manual que utileria suma a una reserva (danio, perdida o servicio).
 *
 * No es un pago: es una linea del detalle. Al cargarse, el backend recalcula el
 * total de la reserva y el saldo de la cuenta, asi caja cobra el monto correcto.
 */
public record CargoRequest(
        @NotNull(message = "El tipo de cargo es obligatorio")
        TipoDetalle tipo,

        @NotBlank(message = "La descripcion del cargo es obligatoria")
        @Size(max = 150, message = "La descripcion no puede superar los 150 caracteres")
        String descripcion,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
        BigDecimal precioUnitario
) {
}
