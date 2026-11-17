package com.proyectosw.sreservas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Entrenamiento;

public interface EntrenamientoRepository extends JpaRepository<Entrenamiento, Integer> {

    /** Los mas proximos primero; el frontend separa vigentes de cancelados. */
    List<Entrenamiento> findAllByOrderByOcupacion_FechaHoraInicioAsc();

    boolean existsByOcupacion_Cancha_Id(Integer canchaId);
}
