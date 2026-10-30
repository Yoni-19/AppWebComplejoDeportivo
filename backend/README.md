# sreservas — Backend

API REST del Sistema de Reservas de un complejo deportivo. Spring Boot sobre MySQL, con JWT.

## Stack

| Elemento | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Spring Security + JWT (jjwt) | 6.x / 0.12.6 |
| JPA / Hibernate | 7.4.5 |
| MySQL | 8.0 |

Sin Lombok: getters, setters y constructores se escriben explícitos.

## Requisitos

- JDK 25
- MySQL 8 corriendo en `localhost:3306` (la base `db_complejoArquitectura` se crea sola)
- Maven no hace falta instalado: el proyecto incluye el wrapper (`mvnw.cmd`)

## Cómo correr

```powershell
cd backend
.\mvnw.cmd clean compile      # build oficial
.\mvnw.cmd spring-boot:run    # levanta en http://localhost:8081
.\mvnw.cmd test               # tests (requiere MySQL arriba)
```

### En VS Code

1. Abrí el workspace **raíz** (`AppWebComplejoDeportivo`), no solo `backend/`.
2. Esperá a que la extensión de Java termine de compilar (barra de estado "Java: Ready").
3. Abrí `SreservasApplication.java` y usá el botón **Run**.
4. Alternativa a prueba de balas: `Terminal > Run Task > backend: run`, o el comando Maven
   de arriba.

La base se inicializa sola: `spring.jpa.hibernate.ddl-auto=update` crea el esquema y
`src/main/resources/data.sql` inserta los datos iniciales de forma idempotente
(`INSERT IGNORE`), así que se puede volver a correr sin duplicar nada.

Si cambia una relación existente (por ejemplo, `pago.reserva_id` → `pago.cuenta_id`),
`ddl-auto=update` no migra las filas: hay que borrar `db_complejoArquitectura` y dejar que se
recree con el seed. En una base con datos reales, migrar a mano antes de levantar la app.

## Credenciales de prueba

Las tres cuentas del seed usan la clave `test123` y sus roles salen de `data.sql`:

| Correo | Rol |
|---|---|
| `admin@email.com` | `ADMIN` — Admin Sistema |
| `laura@email.com` | `CLIENTE` — Laura Gomez |
| `pedro@email.com` | `CLIENTE` — Pedro Diaz |

```powershell
curl -X POST http://localhost:8081/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"correo":"admin@email.com","clave":"test123"}'
```

## Endpoints

Todos salvo `POST /api/auth/login` y `POST /api/usuarios` (registro) exigen
`Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Devuelve `{ token }` |
| GET | `/api/auth/me` | Usuario autenticado con su `rol` |
| GET/POST | `/api/usuarios` | Listar / registrar usuario (el alta nace `CLIENTE`; `?rol=CLIENTE` filtra) |
| GET/PUT/DELETE | `/api/usuarios/{id}` | Detalle / actualizar (PUT no exige clave; recepcionista solo `CLIENTE`) / eliminar (solo `ADMIN`) |
| PUT | `/api/usuarios/{id}/rol` | Cambiar el rol (solo `ADMIN`) |
| GET/POST | `/api/canchas` | Listar / crear cancha |
| GET/PUT/DELETE | `/api/canchas/{id}` | Detalle / actualizar / eliminar |
| GET/POST | `/api/reservas` | Listar / crear reserva (el alta exige `metodoPago` y cobra el anticipo) |
| GET/PUT/DELETE | `/api/reservas/{id}` | Detalle / reprogramar / eliminar (solo `ADMIN`) |
| PUT | `/api/reservas/{id}/cancelar` | Baja de negocio: estado `CANCELADA` y cuenta anulada |
| PUT | `/api/reservas/{id}/check-in` | Arranca el turno (`CONFIRMADA → EN_CURSO`), solo personal |
| PUT | `/api/reservas/{id}/check-out` | Cierra el turno (`EN_CURSO → COMPLETADA`), solo personal |
| GET | `/api/reservas/{id}/cuenta` | Cuenta de la reserva: total, saldo, estado y pagos |
| GET/POST | `/api/pagos` | Listar / registrar movimiento (solo `ADMIN` y `RECEPCIONISTA`) |
| GET/PUT/DELETE | `/api/pagos/{id}` | Detalle / actualizar método / eliminar |

## Roles y permisos

El JWT solo lleva el correo: `JwtAuthFilter` carga el usuario en cada request, así que **el rol
nunca viaja en el token** y un cambio de rol aplica al instante. El frontend lo obtiene con
`GET /api/auth/me`.

| Acción | ADMIN | RECEPCIONISTA | CLIENTE |
|---|---|---|---|
| Ver/crear/editar/eliminar canchas | sí | no | no |
| Ver clientes | sí (todos menos él) | sí (solo clientes) | no |
| Crear/editar clientes | sí | sí (solo `CLIENTE`) | no |
| Eliminar clientes y cambiar roles | sí | no | no |
| Ver todas las reservas | sí | sí | solo las propias |
| Crear reserva | sí | sí | sí (a su nombre) |
| Editar reserva | sí | sí | solo las propias |
| Check-in / check-out | sí | sí | no |
| Eliminar reserva (borrado físico) | sí | no | no |
| Registrar cobros (`/api/pagos`) | sí | sí | no |

Un `CLIENTE` puede reprogramar o cancelar **sus** reservas: el `403` por reserva ajena lo aplica
`ReservaService.verificarPropiedad`. Solo una reserva `CONFIRMADA` se cancela; `EN_CURSO`,
`COMPLETADA` y `CANCELADA` no se reprograman (`409`).

El check-in y el check-out los registra la recepción: `CONFIRMADA → EN_CURSO → COMPLETADA`.
El check-in se acepta desde una hora antes del turno y hasta su fin; los turnos que terminan sin
check-in quedan `NO_ASISTIO` la próxima vez que se listan las reservas. El cobro del saldo es un
paso aparte (`POST /api/pagos`): la cuenta se cierra sola cuando el saldo llega a `0`.

Cancelar conserva el historial: la ocupación queda inactiva (el turno se libera) y la cuenta
pasa a `ANULADA` con saldo `0`. El borrado físico (`DELETE /api/reservas/{id}`) es solo para el
`ADMIN` y se lleva pagos, cuenta y detalle.

En la pantalla de clientes, el `ADMIN` ve a todos los usuarios excepto a sí mismo (su cuenta se
edita desde "Mi perfil") y el `RECEPCIONISTA` ve solo `CLIENTE`. El recepcionista puede dar de
alta clientes presenciales y corregir sus datos, pero no al personal, ni roles, ni borrados.
La clave se entrega en mano: **no hay cambio forzado** (riesgo aceptado). Nadie puede cambiar
su propio rol ni eliminar su propia cuenta (`409`).

Un rol sin permiso recibe `403` con el envelope JSON `{ timestamp, status, error, message }`.

Reglas de negocio implementadas:

- **Solapamiento de ocupaciones**: `409 Conflict` si se superpone con otra ocupación de la
  misma cancha. Una ocupación con `activa = false` no bloquea.
- **Monto**: se cobra por hora iniciada (`ceil` de los minutos / 60), no por hora exacta.
- **Cuenta y anticipo**: cada reserva nace con una `Cuenta` 1:1 y un anticipo del 50 % del
  total. El saldo queda pendiente y se cobra con `POST /api/pagos` (`tipo: SALDO`).
- **Detalle tipo pedido**: `DetalleReserva` guarda las líneas (hoy la línea `CANCHA`) con el
  precio del momento. `Reserva.montoTotal` es la suma de esas líneas.
- **Contratos**: las respuestas usan DTOs (`dto/`), nunca las entidades. El hash BCrypt de
  la clave nunca sale por la API.
- **Último administrador**: no se puede quitar el rol al último `ADMIN` (`409`).
- **CORS**: habilitado para `http://localhost:4201` y `http://127.0.0.1:4201` (frontend Angular).

## Gotcha importante: el IDE escribe en `target/classes`

La extensión *Language Support for Java* de VS Code compila en segundo plano y escribe en el
mismo `target/classes` que Maven. Ese bytecode puede quedar sin la bandera `-parameters`, y
entonces Spring no puede resolver un `@PathVariable Integer id` sin nombre explícito:

```
IllegalArgumentException: Name for argument of type [java.lang.Integer] not specified
```

El síntoma es un `500 Internal Server Error` en cualquier `PUT`/`DELETE` con id de ruta,
aunque todo compile y los tests del servicio pasen.

**Solución.** El build oficial es Maven: borrá `target/` y corré

```powershell
.\mvnw.cmd clean compile
```

`.\mvnw.cmd test` recompila con `[debug parameters release 25]`, que es lo correcto. Si `clean`
falla con `Failed to delete ...`, cerrá la ventana de VS Code o borrá `target/` a mano: el
language server tiene los archivos abiertos.

`.\mvnw.cmd clean` con la app corriendo falla siempre por archivos bloqueados; en ese caso usá
solo `.\mvnw.cmd compile`.
