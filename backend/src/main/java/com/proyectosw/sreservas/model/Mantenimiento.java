package com.proyectosw.sreservas.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

/**
 * Bloqueo de una cancha por mantenimiento.
 *
 * La ocupacion (tipo MANTENIMIENTO) puede no tener fecha de fin: mientras siga
 * activa bloquea la cancha y cualquier reserva futura choca contra ella. Al
 * resolver el mantenimiento la ocupacion se libera pero no se borra, para
 * conservar el historial del bloqueo.
 */
@Entity
public class Mantenimiento {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull (message = "El mantenimiento debe indicar una ocupacion")
    @OneToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "ocupacion_id", unique = true, nullable = false)
    private Ocupacion ocupacion;

    @Column (nullable = false, length = 300)
    private String motivo;

    /** Trabajo realizado para liberar la cancha; se carga al resolver el bloqueo. */
    @Column (length = 500)
    private String solucion;

    @Column (nullable = false, columnDefinition = "boolean not null default false")
    private boolean resuelto = false;

    @Column (nullable = false)
    private LocalDateTime fechaCreacion;

    @Column
    private LocalDateTime fechaResolucion;

    /** Cuantas reservas vigentes quedaron dentro del bloqueo al abrirlo. */
    @Column (nullable = false, columnDefinition = "int not null default 0")
    private int reservasAfectadas = 0;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "reportado_por_id")
    private Usuario reportadoPor;

    public Mantenimiento() {
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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getSolucion() {
        return solucion;
    }

    public void setSolucion(String solucion) {
        this.solucion = solucion;
    }

    public boolean isResuelto() {
        return resuelto;
    }

    public void setResuelto(boolean resuelto) {
        this.resuelto = resuelto;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public int getReservasAfectadas() {
        return reservasAfectadas;
    }

    public void setReservasAfectadas(int reservasAfectadas) {
        this.reservasAfectadas = reservasAfectadas;
    }

    public Usuario getReportadoPor() {
        return reportadoPor;
    }

    public void setReportadoPor(Usuario reportadoPor) {
        this.reportadoPor = reportadoPor;
    }
}
