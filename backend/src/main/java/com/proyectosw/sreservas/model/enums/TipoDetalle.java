package com.proyectosw.sreservas.model.enums;

/**
 * Tipo de linea del detalle de una reserva.
 *
 * CANCHA: la linea base, calculada por hora iniciada.
 * IMPLEMENTO: prestamos sin costo de alquiler.
 * DANIO y PERDIDA: cargos que se agregan al cerrar la reserva.
 * SERVICIO: cualquier extra del complejo (iluminacion, arbitro, etc.).
 */
public enum TipoDetalle {
    CANCHA, IMPLEMENTO, DANIO, PERDIDA, SERVICIO
}
