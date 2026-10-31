package com.proyectosw.sreservas.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Catalogo de implementos que el complejo presta sin costo (pelotas, redes, petos).
 *
 * No hay control de stock: el centro deportivo administra su inventario por fuera
 * del sistema. El catalogo solo sirve para que el cliente los elija al reservar y
 * para que utileria registre la entrega y la devolucion.
 */
@Entity
public class Implemento {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank (message = "El nombre del implemento es obligatorio")
    @Size (max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Column (nullable = false, length = 80, unique = true)
    private String nombre;

    /**
     * Monto sugerido para reponer una unidad perdida o danada. Se usa como
     * precio por defecto del cargo, no como inventario ni como deposito.
     */
    @Column (nullable = false)
    private BigDecimal valorReposicion = BigDecimal.ZERO;

    /** Un implemento inactivo no se ofrece al reservar. */
    @Column (nullable = false)
    private boolean activo = true;

    public Implemento() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getValorReposicion() {
        return valorReposicion;
    }

    public void setValorReposicion(BigDecimal valorReposicion) {
        this.valorReposicion = valorReposicion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
