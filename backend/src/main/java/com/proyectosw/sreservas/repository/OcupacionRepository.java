package com.proyectosw.sreservas.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proyectosw.sreservas.model.Ocupacion;

public interface OcupacionRepository extends JpaRepository<Ocupacion, Integer> {

    /**
     * True si alguna ocupacion activa de la cancha se superpone con [inicio, fin].
     * Una ocupacion sin fecha de fin (mantenimiento abierto) se trata como infinita.
     * {@code ocupacionPropiaId} (nullable) excluye la ocupacion que se esta reprogramando.
     */
    @Query("""
            select count(o) > 0
            from Ocupacion o
            where o.cancha.id = :canchaId
              and o.activa = true
              and o.fechaHoraInicio < :fin
              and (o.fechaHoraFin is null or o.fechaHoraFin > :inicio)
              and (:ocupacionPropiaId is null or o.id <> :ocupacionPropiaId)
            """)
    boolean existeSolapamiento(@Param("canchaId") Integer canchaId,
                               @Param("ocupacionPropiaId") Integer ocupacionPropiaId,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fin") LocalDateTime fin);

    /** True si la cancha tiene cualquier ocupacion (incluso liberada); bloquea su borrado. */
    boolean existsByCancha_Id(Integer canchaId);
}
