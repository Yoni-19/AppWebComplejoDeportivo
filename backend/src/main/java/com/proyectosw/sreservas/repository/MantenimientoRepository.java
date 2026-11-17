package com.proyectosw.sreservas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Mantenimiento;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Integer> {

    /** Los abiertos primero y, dentro de cada grupo, el inicio mas reciente. */
    List<Mantenimiento> findAllByOrderByResueltoAscOcupacion_FechaHoraInicioDesc();

    boolean existsByOcupacion_Cancha_IdAndResueltoFalse(Integer canchaId);

    boolean existsByOcupacion_Cancha_Id(Integer canchaId);
}
