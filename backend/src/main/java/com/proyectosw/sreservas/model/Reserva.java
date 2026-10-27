package com.proyectosw.sreservas.model;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.enums.EstadoServicio;

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
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

/**
 * Reserva de un espacio deportivo hecha por un cliente.
 *
 * La cancha y el horario viven en {@link Ocupacion}; la reserva solo aporta el
 * cliente, el estado y el monto. Asi el calendario tiene una sola fuente de verdad.
 */
@Entity
public class Reserva {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La reserva debe indicar una ocupacion")
    @OneToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "ocupacion_id", unique = true, nullable = false)
    private Ocupacion ocupacion;

    @NotNull (message = "La reserva debe indicar un usuario")
    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column (nullable = false)
    @Enumerated (EnumType.STRING)
    private EstadoServicio estado;

    @Column (nullable = false)
    private BigDecimal montoTotal;

    public Reserva() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Ocupacion getOcupacion() {
        return ocupacion;
    }

    public void setOcupacion(Ocupacion ocupacion) {
        this.ocupacion = ocupacion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EstadoServicio getEstado() {
        return estado;
    }

    public void setEstado(EstadoServicio estado) {
        this.estado = estado;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }
}
