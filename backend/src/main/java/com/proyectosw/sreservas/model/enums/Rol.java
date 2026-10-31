package com.proyectosw.sreservas.model.enums;

/**
 * Rol del usuario dentro del sistema.
 *
 * ADMIN: control total (canchas, implementos, clientes, usuarios, roles).
 * RECEPCIONISTA: opera reservas, registra check-in y check-out y da de alta clientes.
 * UTILERIA: entrega y recibe implementos y carga los danios o perdidas.
 * CAJA: cobra el saldo y los cargos de utileria.
 * CLIENTE: accede unicamente a sus propias reservas.
 *
 * Los valores deben entrar en la columna {@code varchar(20)} de {@code usuario.rol}.
 */
public enum Rol {
    ADMIN, RECEPCIONISTA, UTILERIA, CAJA, CLIENTE
}
