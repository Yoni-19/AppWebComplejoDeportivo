package com.proyectosw.sreservas.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoServicio;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    /** True if the user has any reservation; blocks the deletion of the user. */
    boolean existsByUsuario_Id(Integer usuarioId);

    /** Reservations of a single user: the client only sees their own. */
    List<Reserva> findByUsuario_Id(Integer usuarioId);

    /** Used to expire the shifts that ended without a check-in. */
    List<Reserva> findByEstado(EstadoServicio estado);

    /**
     * Reservas vigentes de una cancha que caen dentro de un bloqueo abierto que
     * empieza en {@code inicio}. Se usa al abrir un mantenimiento para saber a
     * quienes hay que avisar antes de que lleguen al complejo.
     */
    @Query("""
            select r
            from Reserva r
            where r.ocupacion.cancha.id = :canchaId
              and r.ocupacion.activa = true
              and r.estado in :estados
              and (r.ocupacion.fechaHoraFin is null or r.ocupacion.fechaHoraFin > :inicio)
            order by r.ocupacion.fechaHoraInicio asc
            """)
    List<Reserva> findVigentesDesde(@Param("canchaId") Integer canchaId,
                                    @Param("estados") List<EstadoServicio> estados,
                                    @Param("inicio") LocalDateTime inicio);
}
