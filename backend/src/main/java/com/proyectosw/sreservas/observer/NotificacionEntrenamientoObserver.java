package com.proyectosw.sreservas.observer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.proyectosw.sreservas.model.Entrenamiento;
import com.proyectosw.sreservas.model.InscripcionEntrenamiento;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.enums.TipoNotificacion;
import com.proyectosw.sreservas.service.NotificacionService;

/**
 * Convierte los eventos del entrenamiento en notificaciones para los inscriptos.
 *
 * Solo avisa a las inscripciones vigentes: quien ya habia cancelado su cupo no
 * necesita enterarse del cambio.
 */
@Component
public class NotificacionEntrenamientoObserver implements EntrenamientoObserver {

    private final NotificacionService notificacionService;

    public NotificacionEntrenamientoObserver(final NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Override
    public void alCancelar(Entrenamiento entrenamiento, List<InscripcionEntrenamiento> inscripciones) {
        Ocupacion ocupacion = entrenamiento.getOcupacion();
        String referencia = "ENTRENAMIENTO:" + entrenamiento.getId();
        for (InscripcionEntrenamiento inscripcion : inscripciones) {
            notificacionService.enviar(
                    inscripcion.getUsuario(),
                    TipoNotificacion.ENTRENAMIENTO_CANCELADO,
                    "Entrenamiento cancelado",
                    "Se cancelo \"" + entrenamiento.getTitulo() + "\" del "
                            + ocupacion.getFechaHoraInicio() + " en " + ocupacion.getCancha().getNombre() + ".",
                    referencia);
        }
    }

    @Override
    public void alReprogramar(Entrenamiento entrenamiento, List<InscripcionEntrenamiento> inscripciones) {
        Ocupacion ocupacion = entrenamiento.getOcupacion();
        String referencia = "ENTRENAMIENTO:" + entrenamiento.getId();
        for (InscripcionEntrenamiento inscripcion : inscripciones) {
            notificacionService.enviar(
                    inscripcion.getUsuario(),
                    TipoNotificacion.ENTRENAMIENTO_REPROGRAMADO,
                    "Entrenamiento reprogramado",
                    "\"" + entrenamiento.getTitulo() + "\" paso al "
                            + ocupacion.getFechaHoraInicio() + " en " + ocupacion.getCancha().getNombre() + ".",
                    referencia);
        }
    }
}
