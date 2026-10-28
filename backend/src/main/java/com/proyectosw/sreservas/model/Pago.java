package com.proyectosw.sreservas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;

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
 * Movimiento de plata de una cuenta (anticipo, saldo o reembolso).
 *
 * El monto no lo elige el cliente: lo calcula la capa de servicio a partir
 * de la cuenta. Los cargos por cancha o perdidas no son pagos: viven en
 * {@link DetalleReserva}.
 */
@Entity
public class Pago {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column (nullable = false)
    private BigDecimal monto;

    @NotNull (message = "El tipo de pago es obligatorio")
    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private TipoPago tipo;

    @NotNull (message = "El método de pago es obligatorio")
    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;

    @Column (nullable = false)
    private LocalDateTime fechaPago;

    @NotNull (message = "El pago debe referenciar una cuenta")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    public Pago() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public TipoPago getTipo() {
        return tipo;
    }

    public void setTipo(TipoPago tipo) {
        this.tipo = tipo;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public void setCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
    }
}
