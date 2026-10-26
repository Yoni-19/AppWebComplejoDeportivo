package com.proyectosw.sreservas.model;

import com.proyectosw.sreservas.model.enums.Rol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Usuario {
    @Id @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank 
    @Size (min = 2, max = 100)
    @Column (nullable = false, length = 100)
    private String nombres;

    @NotBlank 
    @Size (min = 2, max = 100)
    @Column (nullable = false, length = 100)
    private String apellidos;

    @NotBlank 
    @Email (message = "El correo debe tener un formato válido")
    @Size (max = 100)
    @Column (nullable = false, length = 100, unique = true)
    private String correo;
    
    @Size (min = 2, max = 100)
    @Column (nullable = false, length = 100)
    private String clave;

    /**
     * Rol con el que opera el usuario. El `default` de la columna no es decorativo: sin el,
     * el `alter table` que agrega la columna deja `''` en las filas existentes y Hibernate
     * revienta al convertir `''` a enum.
     */
    @Enumerated (EnumType.STRING)
    @Column (name = "rol", nullable = false, length = 20,
            columnDefinition = "varchar(20) not null default 'CLIENTE'")
    private Rol rol = Rol.CLIENTE;

    public Usuario() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

}
