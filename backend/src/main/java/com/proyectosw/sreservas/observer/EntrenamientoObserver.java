package com.proyectosw.sreservas.observer;

import java.util.List;

import com.proyectosw.sreservas.model.Entrenamiento;
import com.proyectosw.sreservas.model.InscripcionEntrenamiento;

/**
 * Observer de los cambios de un entrenamiento.
 *
 * El servicio notifica a todos los observers registrados por Spring cuando el
 * turno se cancela o se reprograma; hoy el unico interesado es la bandeja de
 * notificaciones, pero el entrenamiento no conoce esa implementacion.
 */
public interface EntrenamientoObserver {
    void alCancelar(Entrenamiento entrenamiento, List<InscripcionEntrenamiento> inscripciones);

    void alReprogramar(Entrenamiento entrenamiento, List<InscripcionEntrenamiento> inscripciones);
}
