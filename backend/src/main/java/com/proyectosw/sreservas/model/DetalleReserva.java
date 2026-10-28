package com.proyectosw.sreservas.model;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.enums.TipoDetalle;

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
import jakarta.validation.constraints.Size;

/**
 * Linea del detalle de una reserva, al estilo del detalle de un pedido.
 *
 * Guarda la descripcion y el precio unitario del momento en que se genero:
 * asi un cambio de precio de la cancha no altera comprobantes viejos.
 */
@Entity
public class DetalleReserva {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La linea debe referenciar una reserva")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "reserva_id", nullable = false)
    private Reserva reserva;

    @NotNull (message = "La linea debe indicar un tipo")
    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private TipoDetalle tipo;

    @Size (max = 150)
    @Column (nullable = false, length = 150)
    private String descripcion;

    @Column (nullable = false)
    private Integer cantidad;

    @Column (nullable = false)
    private BigDecimal precioUnitario;

    @Column (nullable = false)
    private BigDecimal subtotal;

    public DetalleReserva() {
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

    public TipoDetalle getTipo() {
        return tipo;
    }

    public void setTipo(TipoDetalle tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
