package com.proyectosw.sreservas.model;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.TipoOcupacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 * Registro unico de ocupacion de una cancha.
 *
 * Es la unica fuente de verdad del espacio y el horario: reserva, entrenamiento y
 * mantenimiento apuntan a una ocupacion. El chequeo de solapamiento se hace una sola
 * vez sobre esta tabla, sin importar cuantos procesos compitan por la cancha.
 */
@Entity
public class Ocupacion {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La ocupacion debe indicar una cancha")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "cancha_id", nullable = false)
    private Cancha cancha;

    @NotNull (message = "La fecha y hora de inicio es obligatoria")
    @Column (nullable = false)
    private LocalDateTime fechaHoraInicio;

    /**
     * Nulo significa bloqueo abierto sin fecha de fin conocida. Solo lo usa el
     * mantenimiento: la cancha queda inhabilitada hasta que se registre la solucion.
     */
    @Column
    private LocalDateTime fechaHoraFin;

    @NotNull (message = "El tipo de ocupacion es obligatorio")
    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20,
            columnDefinition = "varchar(20) not null default 'RESERVA'")
    private TipoOcupacion tipo;

    /** Una ocupacion liberada deja de bloquear el horario pero conserva el historial. */
    @Column (nullable = false, columnDefinition = "boolean not null default true")
    private boolean activa = true;

    public Ocupacion() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public void setCancha(Cancha cancha) {
        this.cancha = cancha;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public TipoOcupacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoOcupacion tipo) {
        this.tipo = tipo;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
