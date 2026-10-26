package com.proyectosw.sreservas.dto;

import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.MetodoPago;

import jakarta.validation.constraints.NotNull;

/**
 * Create/update payload for a payment. References the reservation by id;
 * {@code monto}, {@code estadoPago} and {@code fechaPago} are managed
 * by the service and therefore excluded.
 */
public record PagoRequest(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @NotNull(message = "El pago debe referenciar una reserva")
        Integer reservaId
) {
    public Pago toEntity() {
        Pago pago = new Pago();
        pago.setMetodoPago(metodoPago);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        pago.setReserva(reserva);

        return pago;
    }
}
