package com.proyectosw.sreservas.model;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.TipoMovimientoImplemento;

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
import jakarta.validation.constraints.Positive;

/**
 * Constancia de una entrega o devolucion de implementos en una reserva.
 *
 * Guarda quien lo registro y cuando, para poder explicar despues un cargo por
 * perdida o danio. El implemento pedido tambien queda como linea IMPLEMENTO en
 * el detalle de la reserva; este movimiento registra el hecho fisico.
 */
@Entity
public class MovimientoImplemento {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "El movimiento debe referenciar una reserva")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "reserva_id", nullable = false)
    private Reserva reserva;

    @NotNull (message = "El movimiento debe indicar el implemento")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "implemento_id", nullable = false)
    private Implemento implemento;

    @NotNull (message = "El movimiento debe indicar el tipo")
    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private TipoMovimientoImplemento tipo;

    @Positive (message = "La cantidad debe ser mayor a cero")
    @Column (nullable = false)
    private Integer cantidad;

    @Column (nullable = false)
    private LocalDateTime fecha;

    @NotNull (message = "El movimiento debe registrar quien lo hizo")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "usuario_id", nullable = false)
    private Usuario registradoPor;

    public MovimientoImplemento() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Implemento getImplemento() {
        return implemento;
    }

    public void setImplemento(Implemento implemento) {
        this.implemento = implemento;
    }

    public TipoMovimientoImplemento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimientoImplemento tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(Usuario registradoPor) {
        this.registradoPor = registradoPor;
    }
}
