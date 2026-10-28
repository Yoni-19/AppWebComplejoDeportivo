# AGENTS.md — sreservas (backend)

> Este backend vive en `backend/` dentro del monorepo `AppWebComplejoDeportivo`.
> **Todos los comandos de esta página se ejecutan desde `backend/`.** El repo git está un nivel
> arriba: `git status` desde acá muestra el monorepo entero. Convenciones generales del
> monorepo en `../AGENTS.md`.

## Build y run

- **El build oficial es Maven.** `mvn` NO está en el PATH de esta máquina: usá el wrapper.
- `.\mvnw.cmd clean compile` — build completo (45 fuentes).
- `.\mvnw.cmd spring-boot:run` — levanta en `http://localhost:8081`.
- `.\mvnw.cmd test` — tests. Requieren MySQL arriba (el test es un `@SpringBootTest`).
- MySQL: `localhost:3306`, user `root`, pass `mysql`, base `db_complejoArquitectura`.
  El esquema y el seed (`src/main/resources/data.sql`) se crean solos.

### Trampa activa en Windows: `clean` con la app corriendo

Con el backend levantado, `.\mvnw.cmd clean` falla con
`Failed to delete target/generated-test-sources/test-annotations`: el JVM bloquea los archivos
de `target/`. Mientras la app esté en ejecución usá **solo** `.\mvnw.cmd compile`.

`spring-boot-devtools` está en el `pom.xml`: al recompilar, la app **se reinicia sola** con las
clases nuevas. No hace falta bajar y volver a levantar para probar un cambio de código.

## Sin Lombok (desde 2026-10-24)

El proyecto **no usa Lombok**. Las 5 entidades tienen getters, setters y constructor vacío
explícitos; las 14 clases de `config/`, `controllers/` y `service/` tienen constructor propio
en lugar de `@RequiredArgsConstructor`. Al agregar un campo `final` nuevo en cualquiera de esas
clases hay que **agregarlo también al constructor**.

Motivo: el LS de VS Code no puede combinar Lombok con JDK 25 (su Lombok embebido es 1.18.39,
sin soporte de JDK 25) y el modo experimental `java.jdt.ls.javac.enabled: "on"` rompía al leer
AssertJ. Sin Lombok se usa el modo normal del LS.

Estado de `.vscode/settings.json` (raíz y `backend/`):

- `"java.jdt.ls.javac.enabled": "off"` — modo normal. **Requiere *Developer: Reload Window*.**
- `"java.autobuild.enabled": true` — el IDE compila al abrir/guardar para que el botón Run
  encuentre las clases en `target/classes` también en un clon limpio (donde `target/` no
  existe porque está gitignoreado).

## Trampa activa: el IDE compila en `target/classes` sin `-parameters`

El LS escribe en el mismo `target/classes` que Maven. Ese bytecode puede quedar sin la bandera
`-parameters`, y Spring entonces no resuelve `@PathVariable Integer id` sin nombre explícito:

```
IllegalArgumentException: Name for argument of type [java.lang.Integer] not specified
```

El síntoma es un `500 Internal Server Error` en cualquier `PUT`/`DELETE` con id de ruta,
aunque todo compile. Si aparece después de que el IDE recompiló: borrá `target/` y corré
`.\mvnw.cmd clean compile` (o `.\mvnw.cmd test`, que recompila con `[debug parameters release 25]`).

Con autobuild activo, evitá `.\mvnw.cmd clean` mientras VS Code compila (o cerrá la ventana):
los dos procesos se pelean por `target/`. Si `clean` falla por archivos bloqueados, borrá
`target/` a mano y volvé a compilar.

## Convenciones

- Idioma del código y los comentarios: español. Nombres de dominio: `Cancha`, `Ocupacion`,
  `Reserva`, `Cuenta`, `DetalleReserva`, `Pago`, `Usuario`, `EstadoServicio`, `EstadoCuenta`,
  `TipoOcupacion`, `TipoDetalle`, `TipoPago`, `TipoDeporte`. Endpoints en
  inglés/plural (`/api/canchas`, `/api/reservas`, `/api/pagos`, `/api/usuarios`).
- **Nunca serializar entidades**: todo sale por DTOs (`dto/`). `Usuario.clave` es el hash
  BCrypt y jamás debe viajar en una respuesta.
- Errores: `@RestControllerAdvice` centralizado + `server.error.include-message=always`.
- CORS: solo `http://localhost:4201` y `http://127.0.0.1:4201` (frontend Angular).
- Sin atributos de IA en comentarios ni commits. Commits en castellano, formato
  `tipo(ámbito): descripción` (`feat`, `fix`, `refactor`, `docs`, `chore`).
- Java 25 disponible en `C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot`.

## Roles y seguridad

- Enum `Rol` (`model/enums/Rol.java`): `ADMIN`, `RECEPCIONISTA`, `CLIENTE`. La columna
  `usuario.rol` es `varchar(20) not null default 'CLIENTE'`.
- El JWT **solo lleva el correo**. `JwtAuthFilter` resuelve el usuario desde la base en cada
  request (`CurrentUserService`), así que los tokens viejos siguen válidos y un cambio de rol
  aplica al instante. No agregar `rol` al token: obligaría a re-loguear a todos.
- `POST /api/usuarios` es público y **fuerza `CLIENTE`** en `UsuarioService.crear`: nunca confiar
  en un rol que venga del cliente.
- El `RECEPCIONISTA` usa ese mismo alta pública para registrar clientes presenciales y puede
  editar `CLIENTE` con `PUT /api/usuarios/{id}`. `UsuarioService.actualizar` bloquea por fila
  al personal y a su propia cuenta (`403`). No puede borrar cuentas ni cambiar roles.
- La clave del cliente presencial la entrega en mano el personal. **No hay cambio forzado de
  clave** (riesgo aceptado para el alcance actual).
- `PUT /api/usuarios/{id}/rol` (`UsuarioRolRequest`) es el único camino para cambiar roles y
  exige `ADMIN`. Quitar el rol al último `ADMIN` → `409`.
- `GET /api/usuarios`: un `ADMIN` ve a todos **menos a sí mismo**; un `RECEPCIONISTA` ve solo
  `CLIENTE`. El filtro opcional `?rol=CLIENTE` lo usan el selector de reservas y el contador
  del dashboard; un recepcionista lo ignora y sigue recibiendo solo clientes.
- Nadie puede cambiar su propio rol ni eliminar su propia cuenta (`409`). El admin edita sus
  datos desde `PUT /api/usuarios/{id}` (botón "Mi perfil" en el frontend).
- `GET /api/usuarios/{id}`: un recepcionista solo puede abrir fichas de `CLIENTE`; el admin
  puede abrir cualquiera, incluida la propia.
- Matriz en `SecurityConfig`; permisos a nivel de fila (reserva ajena) en `ReservaService`.
  Un `403` pasa por `AccessDeniedHandler` (envelope JSON), no por el handler por defecto.
- `PUT /api/reservas/**` (reprogramar y cancelar) acepta a los tres roles, pero `ReservaService`
  llama a `verificarPropiedad`: un `CLIENTE` solo toca sus reservas (si no, `403`).
  Nunca mover esa validación solo a `SecurityConfig`: ahí no se conoce el dueño de la fila.
- `DELETE /api/reservas/**` es solo `ADMIN`. Es borrado físico de mantenimiento, no una operación
  de negocio: la baja de un servicio es `PUT /api/reservas/{id}/cancelar`.
- Una reserva `COMPLETADA` o `CANCELADA` no se reprograma (`409`). Solo una `CONFIRMADA` se
  cancela; `EN_CURSO`, `COMPLETADA`, `CANCELADA` y `NO_ASISTIO` dan `409`.
- Un token malformado o vencido **no corta la cadena**: `JwtAuthFilter` sigue como anónimo y la
  autorización de cada ruta decide. Las rutas públicas (`/api/auth/login`, `POST /api/usuarios`)
  funcionan igual; las privadas devuelven `401`.
- Los tests de la matriz están en `config/SeguridadPorRolTests` (login real contra el seed).

### Trampa: agregar una columna `NOT NULL` a una tabla con filas

`@Enumerated(STRING)` + `nullable = false` deja `''` en las filas existentes y MySQL rechaza el
`ALTER TABLE`. Por eso `Usuario.rol` usa
`@Column(columnDefinition = "varchar(20) not null default 'CLIENTE'")`: Hibernate agrega la
columna con default y las filas viejas quedan como `CLIENTE`. Si alguna vez hay que migrar una
base existente a mano: `UPDATE usuario SET rol = 'ADMIN' WHERE correo = 'admin@email.com';`.

### Trampa: cambiar una FK existente (P2)

`Pago` pasó de `reserva_id` a `cuenta_id` y `ddl-auto=update` no borra la columna vieja ni
rellena la nueva en las filas existentes. `pago.cuenta_id NOT NULL` sobre una tabla con pagos
falla. En desarrollo: borrar `db_complejoArquitectura` y dejar que se recree con el seed.
En una base con datos reales habría que migrar a mano antes de levantar la app.

## Reglas de negocio

- La cancha y el horario viven en `Ocupacion` (calendario único); `Reserva` solo la
  referencia. Cancelar una reserva deja `Ocupacion.activa = false` **sin borrarla**: el
  historial se conserva y el turno se libera porque el solapamiento solo mira las activas.
- Solapamiento de ocupaciones → `409 Conflict` (`OcupacionRepository.existeSolapamiento`).
  Una ocupación inactiva no bloquea; al reprogramar se excluye la propia ocupación.
- Monto por **hora iniciada**: `horas = (minutos + 59) / 60`. No usar `Duration.toHours()`.
- `DetalleReserva` es el detalle tipo pedido: `Reserva.montoTotal` es la suma de sus líneas.
  Al reprogramar solo se recalcula la línea `CANCHA`; las demás líneas no se tocan.
- `Cuenta` es 1:1 con `Reserva`: guarda `total`, `saldo` y `estado` (ABIERTA/CERRADA/ANULADA).
  El saldo se actualiza en `CuentaService` en cada pago, nunca a mano.
- Alta de reserva: una sola transacción crea `Ocupacion` + `Reserva` + línea `CANCHA` + `Cuenta`
  + anticipo del 50 %. El anticipo es `total × 0.50` redondeado a 2 decimales (`PagoFactory`).
- `Pago` cuelga de la cuenta, no de la reserva. Tipos: `ANTICIPO`, `SALDO`, `REEMBOLSO`.
  Los daños o pérdidas **no** son pagos: son líneas `DANIO` / `PERDIDA` del detalle.
- Cobrar el saldo: `POST /api/pagos` con `tipo: SALDO`; el monto lo calcula el backend.
- Cancelar (`PUT /api/reservas/{id}/cancelar`, sin cuerpo): estado `CANCELADA`, ocupación
  inactiva y cuenta `ANULADA` con saldo `0`. Los pagos quedan como historial; si se devuelve
  plata, se registra un `REEMBOLSO` con `POST /api/pagos`, único movimiento que acepta una
  cuenta `ANULADA`.
- `DELETE /api/reservas/{id}` (solo `ADMIN`) borra pagos, cuenta, detalle, reserva y ocupación,
  en ese orden. No tiene guard de pagos: es limpieza deliberada, no una baja de servicio.
- `PUT /api/usuarios/{id}` acepta clave opcional (`UsuarioUpdateRequest`): si viene `null`,
  la clave no cambia. Un `RECEPCIONISTA` solo puede editar `CLIENTE`; al personal → `403`.
- `POST /api/usuarios` exige clave (`UsuarioRequest`); `PUT` también la acepta y la re-codifica.

## Contratos que el frontend NO puede cambiar

- `POST /api/usuarios` y `PUT /api/usuarios/{id}` devuelven `409` si el correo ya existe.
- `POST /api/usuarios` crea siempre un `CLIENTE`; el rol solo cambia por
  `PUT /api/usuarios/{id}/rol` y solo un `ADMIN` puede hacerlo.
- El `RECEPCIONISTA` puede `POST` (alta pública) y `PUT` de `CLIENTE`; no puede `DELETE`,
  `PUT /rol` ni editar al personal (`403`).
- `DELETE /api/usuarios/{id}` y `DELETE /api/canchas/{id}` devuelven `409` si el registro tiene
  reservas o pagos asociados (`existsByUsuario_Id` / `existsByCancha_Id`).
- `PUT /api/reservas/{id}` **ignora el `usuarioId`**: el cliente de una reserva existente no se
  puede cambiar. `estado` tampoco se puede enviar: el backend lo calcula.
- `POST /api/reservas` exige `metodoPago` y crea el anticipo del 50 %; `PUT` lo ignora.
- `PUT /api/reservas/{id}/cancelar` es la única baja de negocio y devuelve la reserva con
  `estado: CANCELADA`. `DELETE /api/reservas/{id}` es solo `ADMIN`; al `RECEPCIONISTA` y al
  `CLIENTE` les devuelve `403`.
- `GET /api/reservas/{id}/cuenta` devuelve total, saldo, estado y pagos; un `CLIENTE` solo
  puede consultar la cuenta de sus propias reservas (`403` igual que con la reserva).
- `POST /api/pagos` es solo para `ADMIN` / `RECEPCIONISTA`. El cliente paga el anticipo al
  crear la reserva; el saldo lo cobra el personal.
- Cualquier violación de FK que se escape a la base devuelve `409` (handler de
  `DataIntegrityViolationException`), nunca `500`.
