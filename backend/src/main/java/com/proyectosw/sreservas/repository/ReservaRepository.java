package com.proyectosw.sreservas.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoReserva;

public interface ReservaRepository extends JpaRepository<Reserva, Integer>{

    /**
     * True if a reservation other than {@code reservaPropiaId} (nullable) occupies
     * the same court over [inicio, fin]. CANCELADA never blocks the slot.
     */
    @Query("""
            select count(r) > 0
            from Reserva r
            where r.cancha.id = :canchaId
              and r.estadoReserva in :estados
              and r.fechaHoraInicio < :fin
              and r.fechaHoraFin > :inicio
              and (:reservaPropiaId is null or r.id <> :reservaPropiaId)
            """)
    boolean existeSolapamiento(@Param("canchaId") Integer canchaId,
                               @Param("reservaPropiaId") Integer reservaPropiaId,
                               @Param("estados") Collection<EstadoReserva> estados,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fin") LocalDateTime fin);

    /** True if the user has any reservation; blocks the deletion of the user. */
    boolean existsByUsuario_Id(Integer usuarioId);

    /** True if the court has any reservation; blocks the deletion of the court. */
    boolean existsByCancha_Id(Integer canchaId);

    /** Reservations of a single user: the client only sees their own. */
    List<Reserva> findByUsuario_Id(Integer usuarioId);
}
