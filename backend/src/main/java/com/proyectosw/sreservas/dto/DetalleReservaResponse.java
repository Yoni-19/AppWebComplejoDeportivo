package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.DetalleReserva;
import com.proyectosw.sreservas.model.enums.TipoDetalle;

/**
 * Linea del detalle de una reserva, al estilo del detalle de un pedido.
 */
public record DetalleReservaResponse(
        Integer id,
        TipoDetalle tipo,
        String descripcion,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetalleReservaResponse toResponse(DetalleReserva detalle) {
        return new DetalleReservaResponse(
                detalle.getId(),
                detalle.getTipo(),
                detalle.getDescripcion(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }
}
