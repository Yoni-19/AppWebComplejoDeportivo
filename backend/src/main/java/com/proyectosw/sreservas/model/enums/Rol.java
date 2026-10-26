package com.proyectosw.sreservas.model.enums;

/**
 * Rol del usuario dentro del sistema.
 *
 * ADMIN: control total (canchas, clientes, usuarios, roles).
 * RECEPCIONISTA: opera reservas y cobra, pero no gestiona canchas, clientes ni roles.
 * CLIENTE: accede unicamente a sus propias reservas.
 */
public enum Rol {
    ADMIN, RECEPCIONISTA, CLIENTE
}
