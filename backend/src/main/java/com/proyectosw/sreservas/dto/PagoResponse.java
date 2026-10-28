package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;

/**
 * Movimiento de cuenta. La cuenta y la reserva se aplanan a sus ids para
 * evitar ciclos de serializacion.
 */
public record PagoResponse(
        Integer id,
        BigDecimal monto,
        TipoPago tipo,
        MetodoPago metodoPago,
        EstadoPago estadoPago,
        LocalDateTime fechaPago,
        Integer cuentaId,
        Integer reservaId
) {
    public static PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getMonto(),
                pago.getTipo(),
                pago.getMetodoPago(),
                pago.getEstadoPago(),
                pago.getFechaPago(),
                pago.getCuenta().getId(),
                pago.getCuenta().getReserva().getId()
        );
    }
}
