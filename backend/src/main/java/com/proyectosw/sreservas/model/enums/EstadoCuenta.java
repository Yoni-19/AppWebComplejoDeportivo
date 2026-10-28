package com.proyectosw.sreservas.model.enums;

/**
 * Estado de la cuenta de una reserva.
 *
 * ABIERTA: queda saldo por cobrar.
 * CERRADA: el saldo llego a cero.
 * ANULADA: la reserva se cancelo y la cuenta deja de cobrarse.
 */
public enum EstadoCuenta {
    ABIERTA, CERRADA, ANULADA
}
