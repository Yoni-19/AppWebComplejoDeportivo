# Sistema Complejo Deportivo

Sistema de reservas de canchas de un complejo deportivo: API REST en Spring Boot sobre MySQL
y SPA en Angular. Un solo repositorio con las dos mitades.

## Estructura

```
.
├── backend/     API REST (Spring Boot + MySQL + JWT)
├── frontend/    SPA (Angular standalone + signals + Material)
├── .vscode/     configuración compartida del workspace
├── README.md    este archivo
└── AGENTS.md    convenciones del monorepo
```

Cada subproyecto tiene su propio `.gitignore` y su documentación:
`backend/README.md` y `frontend/README.md`.

## Stack

| Parte | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1.1, Spring Security + JWT, Spring Data JPA, MySQL 8.0 |
| Frontend | Angular 22 (standalone, signals), Angular Material 22, TypeScript 6, RxJS 7.8 |

## Requisitos

- JDK 25
- MySQL 8 corriendo en `localhost:3306` — la base `db_complejoArquitectura` se crea sola
- Node.js 20+ con npm
- Maven no hace falta: el backend trae el wrapper (`backend/mvnw.cmd`)

## Cómo correr

Son **dos procesos separados**. Primero el backend, porque el frontend consume la API real.

```powershell
# Terminal 1 — API en http://localhost:8081
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2 — SPA en http://localhost:4201
cd frontend
npm install
npm start
```

La base se inicializa sola: `ddl-auto=update` crea el esquema y `backend/src/main/resources/data.sql`
inserta los datos iniciales de forma idempotente (`INSERT IGNORE`), así que se puede volver a
correr sin duplicar nada.

## Credenciales de prueba

Las cuentas del seed usan la clave `test123`:

| Correo | Rol | Nombre |
|---|---|---|
| `admin@email.com` | `ADMIN` | Admin Sistema |
| `laura@email.com` | `CLIENTE` | Laura Gomez |
| `pedro@email.com` | `CLIENTE` | Pedro Diaz |

Cualquiera puede registrarse desde `/registro`: el alta siempre nace como `CLIENTE` y un
`ADMIN` puede ascenderla desde la pantalla de clientes.

## Reglas de negocio

- **Roles**: `ADMIN` ⊃ `RECEPCIONISTA` ⊃ `CLIENTE`. El JWT solo lleva el correo y el rol se
  lee de la base en cada request, así que un cambio de rol aplica al instante.
- **Solapamiento**: `409 Conflict` si una reserva se superpone con otra de la misma cancha.
- **Monto**: por hora iniciada (`ceil(minutos / 60)`), no por hora exacta.
- **Contratos**: las respuestas usan DTOs; el hash BCrypt de la clave nunca sale por la API.
- **CORS**: habilitado solo para `http://localhost:4201` y `http://127.0.0.1:4201`.

El detalle de los endpoints y de la matriz de permisos está en `backend/README.md`.

## Estado actual

Roles implementados en backend y frontend (menú, rutas, dashboard y formularios por rol).
Pendiente: la regla del abono del 50% en el módulo de pagos.
