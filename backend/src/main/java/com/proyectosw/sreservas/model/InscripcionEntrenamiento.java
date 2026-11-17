package com.proyectosw.sreservas.model;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.EstadoInscripcion;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Inscripcion de un cliente a un entrenamiento.
 *
 * Un cliente tiene a lo sumo una inscripcion por entrenamiento: si la cancela
 * y vuelve a inscribirse se reutiliza la misma fila y se cambia el estado.
 */
@Entity
@Table (uniqueConstraints = @UniqueConstraint (
        name = "uk_inscripcion_entrenamiento_usuario",
        columnNames = { "entrenamiento_id", "usuario_id" }))
public class InscripcionEntrenamiento {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La inscripcion debe indicar un entrenamiento")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "entrenamiento_id", nullable = false)
    private Entrenamiento entrenamiento;

    @NotNull (message = "La inscripcion debe indicar un cliente")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20,
            columnDefinition = "varchar(20) not null default 'INSCRITO'")
    private EstadoInscripcion estado = EstadoInscripcion.INSCRITO;

    @Column (nullable = false)
    private LocalDateTime fechaInscripcion;

    public InscripcionEntrenamiento() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Entrenamiento getEntrenamiento() {
        return entrenamiento;
    }

    public void setEntrenamiento(Entrenamiento entrenamiento) {
        this.entrenamiento = entrenamiento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDateTime fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }
}
