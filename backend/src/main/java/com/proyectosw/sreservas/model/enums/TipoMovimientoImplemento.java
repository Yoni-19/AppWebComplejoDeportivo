package com.proyectosw.sreservas.model.enums;

/**
 * Movimiento fisico de un implemento en una reserva.
 *
 * ENTREGA: utileria le entrega la unidad al cliente.
 * DEVOLUCION: el cliente la devuelve (incluye las que vuelven danadas).
 *
 * No es inventario: no descuenta stock, solo deja constancia de que el
 * implemento salio y volvio, con quien lo registro y cuando.
 */
public enum TipoMovimientoImplemento {
    ENTREGA, DEVOLUCION
}
