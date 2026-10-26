package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.model.enums.MetodoPago;

/**
 * Payment view. The nested {@code Reserva} is flattened to its id
 * to avoid serialization cycles.
 */
public record PagoResponse(
        Integer id,
        BigDecimal monto,
        MetodoPago metodoPago,
        EstadoPago estadoPago,
        LocalDateTime fechaPago,
        Integer reservaId
) {
    public static PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getMonto(),
                pago.getMetodoPago(),
                pago.getEstadoPago(),
                pago.getFechaPago(),
                pago.getReserva().getId()
        );
    }
}
