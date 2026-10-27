package com.proyectosw.sreservas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /** True if the user has any reservation; blocks the deletion of the user. */
    boolean existsByUsuario_Id(Integer usuarioId);

    /** Reservations of a single user: the client only sees their own. */
    List<Reserva> findByUsuario_Id(Integer usuarioId);
}
