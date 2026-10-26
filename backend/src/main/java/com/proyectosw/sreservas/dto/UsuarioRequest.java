package com.proyectosw.sreservas.dto;

import com.proyectosw.sreservas.model.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100)
        String nombres,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(min = 2, max = 100)
        String apellidos,

        @NotBlank
        @Email(message = "El correo debe tener un formato válido")
        @Size(max = 100)
        String correo,

        @NotBlank(message = "La clave es obligatoria")
        @Size(min = 2, max = 100)
        String clave
) {
    public Usuario toEntity() {
        Usuario usuario = new Usuario();
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setClave(clave);
        return usuario;
    }
}
