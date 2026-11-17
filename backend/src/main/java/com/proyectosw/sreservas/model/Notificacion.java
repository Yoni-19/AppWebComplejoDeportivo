package com.proyectosw.sreservas.model;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.enums.TipoNotificacion;

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
 * Aviso interno para un usuario.
 *
 * El patron Observer crea estas filas cuando un entrenamiento se cancela o se
 * reprograma y cuando se abre o resuelve un mantenimiento. El cliente las
 * consulta desde la campana del layout; el backend solo las escribe.
 */
@Entity
public class Notificacion {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "La notificacion debe indicar un destinatario")
    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column (nullable = false, length = 150)
    private String titulo;

    @Column (nullable = false, length = 500)
    private String mensaje;

    @Column (nullable = false, columnDefinition = "boolean not null default false")
    private boolean leida = false;

    @Column (nullable = false)
    private LocalDateTime fechaCreacion;

    /** Referencia opcional al origen, por ejemplo "ENTRENAMIENTO:7". */
    @Column (length = 100)
    private String referencia;

    public Notificacion() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
