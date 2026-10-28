package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;

import jakarta.validation.constraints.NotNull;

/**
 * Alta de un movimiento sobre la cuenta de una reserva.
 *
 * {@code monto} solo se usa en REEMBOLSO: en ANTICIPO y SALDO lo calcula el
 * backend para que el cliente no pueda inflar ni reducir lo que debe.
 */
public record PagoRequest(
        @NotNull(message = "El tipo de pago es obligatorio")
        TipoPago tipo,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @NotNull(message = "El pago debe referenciar una reserva")
        Integer reservaId,

        BigDecimal monto
) {
}
