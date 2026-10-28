package com.proyectosw.sreservas.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.proyectosw.sreservas.model.enums.EstadoCuenta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotNull;

/**
 * Cuenta de una reserva: cuanto se debe y cuanto queda por pagar.
 *
 * Es la cabecera contable. Los cargos que forman el total viven en
 * {@link DetalleReserva} y los movimientos de plata en {@link Pago}.
 * Mantener la cuenta aparte de la reserva permite agregar cargos al cerrar
 * (perdidas, danios) sin tocar la reserva original.
 */
@Entity
public class Cuenta {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La cuenta debe referenciar una reserva")
    @OneToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "reserva_id", unique = true, nullable = false)
    private Reserva reserva;

    @Column (nullable = false)
    private BigDecimal total;

    /** Saldo pendiente. Se recalcula en cada pago o cambio del total. */
    @Column (nullable = false)
    private BigDecimal saldo;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private EstadoCuenta estado = EstadoCuenta.ABIERTA;

    @OneToMany (mappedBy = "cuenta", fetch = FetchType.LAZY)
    @OrderBy ("id ASC")
    private List<Pago> pagos = new ArrayList<>();

    public Cuenta() {
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

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }
}
