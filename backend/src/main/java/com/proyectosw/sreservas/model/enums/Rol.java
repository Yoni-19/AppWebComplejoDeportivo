package com.proyectosw.sreservas.model.enums;

/**
 * Rol del usuario dentro del sistema.
 *
 * ADMIN: control total (canchas, implementos, clientes, usuarios, roles).
 * RECEPCIONISTA: opera reservas, registra check-in y check-out y da de alta clientes.
 * UTILERIA: entrega y recibe implementos y carga los danios o perdidas.
 * CAJA: cobra el saldo y los cargos de utileria.
 * MANTENIMIENTO: encargado de los espacios; su modulo llega en la fase P7, por ahora
 *   solo puede entrar al sistema y consultar canchas.
 * CLIENTE: accede unicamente a sus propias reservas.
 *
 * Los valores deben entrar en la columna {@code varchar(20)} de {@code usuario.rol}:
 * {@code MANTENIMIENTO} ocupa 13 caracteres.
 */
public enum Rol {
    ADMIN, RECEPCIONISTA, UTILERIA, CAJA, MANTENIMIENTO, CLIENTE
}
