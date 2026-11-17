package com.proyectosw.sreservas.model.enums;

/**
 * Estado de la inscripcion de un cliente a un entrenamiento.
 *
 * INSCRITO: ocupa un cupo del entrenamiento.
 * CANCELADO: libero el cupo pero conserva el historial de la inscripcion.
 */
public enum EstadoInscripcion {
    INSCRITO, CANCELADO
}
