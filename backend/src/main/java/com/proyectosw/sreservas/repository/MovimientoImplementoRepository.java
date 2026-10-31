package com.proyectosw.sreservas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.MovimientoImplemento;

public interface MovimientoImplementoRepository extends JpaRepository<MovimientoImplemento, Integer> {

    List<MovimientoImplemento> findByReserva_IdOrderByIdAsc(Integer reservaId);

    boolean existsByImplemento_Id(Integer implementoId);

    void deleteByReserva_Id(Integer reservaId);
}
