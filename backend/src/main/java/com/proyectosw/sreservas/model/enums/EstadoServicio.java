package com.proyectosw.sreservas.model.enums;

/**
 * Estado del ciclo de vida de un servicio reservado (reserva o entrenamiento).
 *
 * CONFIRMADA: creado con el anticipo del 50% pagado.
 * EN_CURSO: el recepcionista registro el check-in.
 * COMPLETADA: se hizo el check-out; el saldo puede quedar pendiente en la cuenta.
 * CANCELADA: se cancelo antes de empezar.
 * NO_ASISTIO: vencio el horario sin check-in.
 */
public enum EstadoServicio {
    CONFIRMADA, EN_CURSO, COMPLETADA, CANCELADA, NO_ASISTIO
}
