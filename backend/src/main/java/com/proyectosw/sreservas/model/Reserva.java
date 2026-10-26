package com.proyectosw.sreservas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.EstadoReserva;

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

@Entity
public class Reserva {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La fecha y hora de inicio es obligatoria")
    @Column (nullable = false)
    private LocalDateTime fechaHoraInicio;

    @NotNull (message = "La fecha y hora de fin es obligatoria")
    @Column (nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoReserva;

    @Column (nullable = false)
    private BigDecimal montoTotal;

    @NotNull (message = "La reserva debe indicar un usuario")
    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @NotNull (message = "La reserva debe indicar una cancha")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Cancha cancha;

    public Reserva() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public EstadoReserva getEstadoReserva() {
        return estadoReserva;
    }

    public void setEstadoReserva(EstadoReserva estadoReserva) {
        this.estadoReserva = estadoReserva;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public void setCancha(Cancha cancha) {
        this.cancha = cancha;
    }
}
