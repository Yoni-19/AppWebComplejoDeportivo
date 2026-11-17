package com.proyectosw.sreservas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    /** Bandeja propia: la mas nueva arriba. */
    List<Notificacion> findByDestinatario_IdOrderByFechaCreacionDesc(Integer destinatarioId);

    long countByDestinatario_IdAndLeidaFalse(Integer destinatarioId);

    List<Notificacion> findByDestinatario_IdAndLeidaFalse(Integer destinatarioId);
}
