package com.proyectosw.sreservas.factory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;

/**
 * Construye los movimientos de una cuenta con sus reglas de monto.
 *
 * El cliente nunca envia el monto: el anticipo es siempre la mitad del total
 * y el saldo es lo que falta. Solo el reembolso recibe un monto explicito,
 * porque depende de la cancelacion.
 */
public final class PagoFactory {

    private static final BigDecimal MITAD = new BigDecimal("0.50");

    private PagoFactory() {
    }

    public static Pago anticipo(Cuenta cuenta, MetodoPago metodoPago) {
        BigDecimal monto = cuenta.getTotal().multiply(MITAD).setScale(2, RoundingMode.HALF_UP);
        return crear(cuenta, TipoPago.ANTICIPO, metodoPago, monto);
    }

    public static Pago saldo(Cuenta cuenta, MetodoPago metodoPago) {
        return crear(cuenta, TipoPago.SALDO, metodoPago, cuenta.getSaldo());
    }

    public static Pago reembolso(Cuenta cuenta, MetodoPago metodoPago, BigDecimal monto) {
        return crear(cuenta, TipoPago.REEMBOLSO, metodoPago, monto);
    }

    private static Pago crear(Cuenta cuenta, TipoPago tipo, MetodoPago metodoPago, BigDecimal monto) {
        Pago pago = new Pago();
        pago.setCuenta(cuenta);
        pago.setTipo(tipo);
        pago.setMetodoPago(metodoPago);
        pago.setMonto(monto);
        pago.setEstadoPago(EstadoPago.PAGADO);
        pago.setFechaPago(LocalDateTime.now());
        return pago;
    }
}
