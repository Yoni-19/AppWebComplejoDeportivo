package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.InscripcionEntrenamiento;
import com.proyectosw.sreservas.model.enums.EstadoInscripcion;

/** Una inscripcion con el nombre del cliente, sin exponer la entidad Usuario. */
public record InscripcionEntrenamientoResponse(
        Integer id,
        Integer entrenamientoId,
        Integer usuarioId,
        String usuarioNombre,
        EstadoInscripcion estado,
        LocalDateTime fechaInscripcion
) {
    public static InscripcionEntrenamientoResponse toResponse(InscripcionEntrenamiento inscripcion) {
        return new InscripcionEntrenamientoResponse(
                inscripcion.getId(),
                inscripcion.getEntrenamiento().getId(),
                inscripcion.getUsuario().getId(),
                inscripcion.getUsuario().getNombres() + " " + inscripcion.getUsuario().getApellidos(),
                inscripcion.getEstado(),
                inscripcion.getFechaInscripcion()
        );
    }
}
