package com.proyectosw.sreservas.dto;

import com.proyectosw.sreservas.model.enums.Rol;

import jakarta.validation.constraints.NotNull;

/** Payload de PUT /api/usuarios/{id}/rol: cambia el rol de un usuario existente. */
public record UsuarioRolRequest(
        @NotNull(message = "El rol es obligatorio")
        Rol rol
) {
}
