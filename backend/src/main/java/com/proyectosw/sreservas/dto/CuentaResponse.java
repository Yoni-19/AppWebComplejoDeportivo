package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.util.List;

import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;

/**
 * Vista de la cuenta de una reserva: total, saldo, estado y movimientos.
 */
public record CuentaResponse(
        Integer id,
        Integer reservaId,
        BigDecimal total,
        BigDecimal saldo,
        EstadoCuenta estado,
        List<PagoResponse> pagos
) {
    public static CuentaResponse toResponse(Cuenta cuenta) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getReserva().getId(),
                cuenta.getTotal(),
                cuenta.getSaldo(),
                cuenta.getEstado(),
                cuenta.getPagos().stream().map(PagoResponse::toResponse).toList()
        );
    }
}
