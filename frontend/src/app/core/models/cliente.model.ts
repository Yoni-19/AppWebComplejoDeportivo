/**
 * Client view of backend Usuario. The `clave` field never leaves the backend
 * (UsuarioResponse DTO); telefono/telefono do not exist in the API either.
 */
export interface Cliente {
  id: number | null;
  nombres: string;
  apellidos: string;
  correo: string;
  /** Always present in API responses; optional for locally built objects. */
  rol?: Rol;
}

/** Role of a user; mirrors the backend enum `Rol`. */
export type Rol = 'ADMIN' | 'RECEPCIONISTA' | 'CLIENTE';

/** Human-readable role names used in chips and menus. */
export const ROL_LABEL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  RECEPCIONISTA: 'Recepcionista',
  CLIENTE: 'Cliente',
};

/** GET /api/auth/me: what the UI needs to know about the current session. */
export interface UsuarioSesion {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  rol: Rol;
}

/** POST /api/usuarios: the backend requires a password on creation. */
export interface ClienteAlta {
  nombres: string;
  apellidos: string;
  correo: string;
  clave: string;
}

/** PUT /api/usuarios/{id}: `clave` is optional; empty keeps the current one. */
export interface ClienteEdicion {
  nombres: string;
  apellidos: string;
  correo: string;
  clave?: string;
}
