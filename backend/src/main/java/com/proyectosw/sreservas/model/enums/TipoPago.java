package com.proyectosw.sreservas.model.enums;

/**
 * Movimiento de plata sobre una cuenta.
 *
 * ANTICIPO y SALDO suman a lo pagado; REEMBOLSO devuelve plata al cliente.
 * Los cargos (cancha, implementos, perdidas) no viven aca: van en DetalleReserva.
 */
public enum TipoPago {
    ANTICIPO, SALDO, REEMBOLSO
}
