package com.proyectosw.sreservas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.DetalleReserva;
import com.proyectosw.sreservas.model.enums.TipoDetalle;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Integer> {

    List<DetalleReserva> findByReserva_Id(Integer reservaId);

    Optional<DetalleReserva> findFirstByReserva_IdAndTipo(Integer reservaId, TipoDetalle tipo);

    void deleteByReserva_Id(Integer reservaId);
}
