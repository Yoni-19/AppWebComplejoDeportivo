package com.proyectosw.sreservas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.model.enums.MetodoPago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class Pago {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column (nullable = false)
    private BigDecimal monto;
    
    @NotNull (message = "El método de pago es obligatorio")
    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;
    
    @Column (nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;
    
    @Column (nullable = false)
    private LocalDateTime fechaPago;
    
    @NotNull (message = "El pago debe referenciar una reserva")
    @OneToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "reserva_id", unique = true, nullable = false)
    private Reserva reserva;

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

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

}
