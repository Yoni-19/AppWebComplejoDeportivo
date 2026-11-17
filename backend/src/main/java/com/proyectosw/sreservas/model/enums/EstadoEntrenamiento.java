package com.proyectosw.sreservas.model.enums;

/**
 * Estado del entrenamiento.
 *
 * PROGRAMADO: el turno esta vigente y se puede inscribir.
 * CANCELADO: el turno se dio de baja; la ocupacion queda liberada y los
 *   inscriptos reciben una notificacion.
 */
public enum EstadoEntrenamiento {
    PROGRAMADO, CANCELADO
}
