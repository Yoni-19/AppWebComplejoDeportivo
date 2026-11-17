package com.proyectosw.sreservas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.InscripcionEntrenamiento;
import com.proyectosw.sreservas.model.enums.EstadoInscripcion;

public interface InscripcionEntrenamientoRepository extends JpaRepository<InscripcionEntrenamiento, Integer> {

    List<InscripcionEntrenamiento> findByEntrenamiento_IdOrderByIdAsc(Integer entrenamientoId);

    Optional<InscripcionEntrenamiento> findByEntrenamiento_IdAndUsuario_Id(Integer entrenamientoId,
            Integer usuarioId);

    long countByEntrenamiento_IdAndEstado(Integer entrenamientoId, EstadoInscripcion estado);

    List<InscripcionEntrenamiento> findByEntrenamiento_IdAndEstado(Integer entrenamientoId,
            EstadoInscripcion estado);
}
