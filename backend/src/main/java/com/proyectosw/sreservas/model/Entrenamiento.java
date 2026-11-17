package com.proyectosw.sreservas.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.proyectosw.sreservas.model.enums.EstadoEntrenamiento;

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
 * Entrenamiento grupal dictado en una cancha.
 *
 * La cancha y el horario viven en {@link Ocupacion} (tipo ENTRENAMIENTO); el
 * entrenamiento aporta el titulo, el entrenador, el cupo y el precio informativo.
 * El cobro no integra la cuenta de una reserva: se muestra el precio y el pago
 * se resuelve fuera del sistema (alcance actual de P6).
 */
@Entity
public class Entrenamiento {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "El entrenamiento debe indicar una ocupacion")
    @OneToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "ocupacion_id", unique = true, nullable = false)
    private Ocupacion ocupacion;

    @Column (nullable = false, length = 120)
    private String titulo;

    @Column (nullable = false, length = 100)
    private String entrenador;

    @Column (length = 300)
    private String descripcion;

    @Column (nullable = false)
    private int cupoMaximo;

    /** Precio informativo por clase; no genera movimientos de caja. */
    @Column (nullable = false, precision = 10, scale = 2,
            columnDefinition = "decimal(10,2) not null default 0.00")
    private BigDecimal precio = BigDecimal.ZERO;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20,
            columnDefinition = "varchar(20) not null default 'PROGRAMADO'")
    private EstadoEntrenamiento estado = EstadoEntrenamiento.PROGRAMADO;

    @OneToMany (mappedBy = "entrenamiento", fetch = FetchType.LAZY)
    @OrderBy ("id ASC")
    private List<InscripcionEntrenamiento> inscripciones = new ArrayList<>();

    public Entrenamiento() {
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEntrenador() {
        return entrenador;
    }

    public void setEntrenador(String entrenador) {
        this.entrenador = entrenador;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(int cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public EstadoEntrenamiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoEntrenamiento estado) {
        this.estado = estado;
    }

    public List<InscripcionEntrenamiento> getInscripciones() {
        return inscripciones;
    }

    public void setInscripciones(List<InscripcionEntrenamiento> inscripciones) {
        this.inscripciones = inscripciones;
    }
}
