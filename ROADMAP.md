# ROADMAP — Sistema Complejo Deportivo

Estado de los procesos del sistema, en el orden en que se están construyendo. Cada fase se
cierra con los tests del backend en verde, el build del frontend OK y la documentación
(`AGENTS.md`, `README.md` de cada subproyecto) alineada con el código.

## Los cinco procesos de negocio

| # | Proceso | Quién lo opera | Dónde vive |
|---|---|---|---|
| 1 | Reserva de un espacio deportivo | Cliente / recepción | `/reservas`, `ReservaService` |
| 2 | Uso del espacio (check-in / check-out) | Recepción | `/reservas`, `ReservaService` |
| 3 | Utilería e implementos | Utilería | `/utileria`, `UtileriaService` |
| 4 | Cobro en caja | Caja | `/caja`, `PagoService` |
| 5 | Entrenamiento deportivo | Cliente / recepción | `/entrenamientos`, `EntrenamientoService` |
| 6 | Mantenimiento de espacios | Mantenimiento | `/mantenimiento`, `MantenimientoService` |

El orden real del turno: **reserva con anticipo del 50 % → check-in → uso → devolución de
implementos con sus cargos → cobro del saldo en caja → check-out (`COMPLETADA`)**.

## Fases

| Fase | Proceso | Estado |
|---|---|---|
| P1 | Calendario único de canchas | Cerrado |
| P2 | Cobros (cuenta, anticipo, saldo) | Cerrado |
| P3 | Check-in / check-out / `NO_ASISTIO` | Cerrado |
| P4 | Utilería e implementos | Implementado (falta prueba manual) |
| P5 | Caja | Implementado (falta prueba manual) |
| P6 | Entrenamientos y patrón Observer | Implementado (falta prueba manual) |
| P7 | Mantenimiento de canchas | Implementado (falta prueba manual) |
| P8 | Diagramas y cierre de roles | Pendiente |

## P1 — Calendario único de canchas (cerrado)

La cancha y el horario de una reserva viven en `Ocupacion` (`cancha`, rango, `tipo`,
`activa`); `Reserva` solo la referencia con `@OneToOne`. El solapamiento se consulta en un
único lugar (`OcupacionRepository.existeSolapamiento`) y se resuelve con `409`.
`EstadoReserva` se reemplazó por `EstadoServicio`: `CONFIRMADA`, `EN_CURSO`, `COMPLETADA`,
`CANCELADA`, `NO_ASISTIO`.

## P2 — Cobros (cerrado)

Cada reserva nace con una `Cuenta` 1:1 y un `DetalleReserva` tipo pedido; `montoTotal` es la
suma de las líneas. El anticipo del 50 % se registra al crear la reserva y el saldo se cobra
después con `POST /api/pagos`. `Pago` cuelga de la cuenta, no de la reserva, y se tipa como
`ANTICIPO`, `SALDO` o `REEMBOLSO`.

## P3 — Check-in / check-out (cerrado)

- `PUT /api/reservas/{id}/check-in`: `CONFIRMADA → EN_CURSO`. Solo personal, desde una hora
  antes del inicio y hasta el fin del turno.
- `PUT /api/reservas/{id}/check-out`: `EN_CURSO → COMPLETADA`. Solo personal.
- `NO_ASISTIO`: los turnos confirmados cuyo horario ya terminó se vencen al listar reservas
  (`ReservaService.listarTodos`), sin scheduler.
- No se reprograma una reserva `EN_CURSO` (`409`).
- Frontend: botones de check-in y check-out en la lista, visibles solo para recepción.

## P4 — Utilería e implementos (implementado)

**Problema**: el préstamo de pelotas, redes y petos era invisible para el sistema. Cuando una
unidad se perdía o se dañaba, el monto no aparecía en la reserva y la caja cobraba de menos.

**Decisión de alcance**: el inventario físico lo administra el complejo por fuera; el sistema
**no** lleva stock. Solo define qué se presta (`Implemento`), qué pidió cada reserva, qué se
entregó, qué volvió y qué se cobra por lo que no volvió.

**Entregado**:

- Catálogo `Implemento` (`nombre`, `valorReposicion`, `activo`) con alta, edición y
  activación/desactivación en `/implementos` (`ADMIN`); todos los roles autenticados pueden
  consultarlo para elegir al reservar. La UI no borra: desactiva.
- Al crear la reserva se eligen implementos con cantidad: quedan como líneas `IMPLEMENTO` con
  precio `0` (el préstamo es gratis). El `PUT` las ignora.
- `MovimientoImplemento` registra la constancia del hecho físico: `ENTREGA` / `DEVOLUCION`,
  cantidad, fecha y quién lo registró. No es inventario, es trazabilidad.
- `GET /api/reservas/{id}/implementos` devuelve pedidos, entregados, devueltos, pendientes y
  el `valorReposicion` con el que se cobraría una unidad dañada o perdida.
- `POST /api/reservas/{id}/implementos/entrega` y `.../devolucion`: utilería registra el
  movimiento. Al devolver, las unidades dañadas generan un cargo `DANIO` y el faltante un
  cargo `PERDIDA`; ambos recalculan el total de la reserva y el saldo de la cuenta.
- `POST /api/reservas/{id}/cargos`: cargo manual `DANIO` / `PERDIDA` / `SERVICIO`.
- Solo se opera sobre un turno `EN_CURSO`: antes del check-in no hay nada prestado y después
  del check-out ya se cerró.
- Frontend: pantalla `/utileria` con selector de turno en curso, entrega, devolución y cargo.
  La devolución separa **unidades en buen estado** de **unidades dañadas** y muestra un resumen
  en vivo (vuelven, dañadas, no vuelven, cargos) antes de registrar; el cargo manual
  (`DANIO` / `PERDIDA` / `SERVICIO`) quedó como acción secundaria en un diálogo que lista los
  cargos ya aplicados, para no cargar dos veces el mismo daño.

## P5 — Caja (implementado)

**Problema**: el importe a cobrar cambiaba después de los cargos de utilería y no había una
pantalla que mostrara "quién debe cuánto" ni que impidiera cerrar un turno con deuda.

**Entregado**:

- Pantalla `/caja`: turnos `EN_CURSO` con cuenta `ABIERTA` y saldo pendiente, total por cobrar y
  cobro del saldo con el método elegido (`POST /api/pagos`, tipo `SALDO`). El monto lo calcula el
  backend, así los daños y pérdidas ya están incluidos. Una reserva `CONFIRMADA` no aparece:
  cobrar antes del uso dejaría la cuenta reabierta si después se carga un cargo.
- El cobro es exclusivo de caja: la lista de reservas ya no cobra y `POST /api/pagos` acepta
  solo `ADMIN` / `CAJA`. El anticipo presencial lo crea `ReservaService` al reservar.
- `PUT /api/reservas/{id}/check-out` rechaza el cierre con `409` si la cuenta tiene saldo.
  El backend permite forzarlo a un `ADMIN` con `?forzar=true` (cliente que se retira sin pagar);
  la UI no le expone check-out al admin.
- Roles `CAJA` y `UTILERIA` agregados a la matriz de seguridad.

**Pendiente para cerrar del todo**: corte de caja diario (total por método y arqueo) y la
prueba manual del circuito completo con los usuarios del seed (`pedro@email.com` en recepción,
`utileria@email.com` y `caja@email.com`, clave `test123`).

## P6 — Entrenamientos y patrón Observer (implementado)

**Problema**: las clases con entrenador no tenían lugar en el calendario y no había forma de
avisar a los alumnos si el turno se cancelaba o se movía.

**Entregado**:

- `Entrenamiento` se apoya en una `Ocupacion` de tipo `ENTRENAMIENTO`, igual que una reserva:
  el solapamiento se sigue chequeando en un único lugar. `InscripcionEntrenamiento` relaciona
  al cliente con el turno y controla el cupo (`INSCRITO` / `CANCELADO`).
- `POST/PUT /api/entrenamientos` y `PUT /{id}/cancelar` los opera `ADMIN` / `RECEPCIONISTA`;
  el catálogo `GET` lo ve cualquier autenticado y el `CLIENTE` se inscribe a sí mismo con
  `POST /{id}/inscripciones` (el backend ignora cualquier usuario del cuerpo).
- Patrón **Observer**: `EntrenamientoService` notifica a `EntrenamientoObserver` al cancelar o
  reprogramar. `NotificacionEntrenamientoObserver` escribe una `Notificacion` por inscripto
  vigente; la campana del layout y la pantalla `/notificaciones` las muestran.
- **Alcance de cobro**: el `precio` es informativo y no integra `Cuenta` / `Pago`. Evita abrir
  el fork de N cuentas por entrenamiento; el pago de la clase se resuelve fuera del sistema.
- UI: `/entrenamientos` con alta/edición y cancelación para recepción; el admin lo ve como
  catálogo de solo lectura y el cliente se inscribe o libera su cupo. El listado muestra cupos,
  precio, estado e inscriptos.

## P7 — Mantenimiento de canchas (implementado)

**Problema**: una cancha rota seguía apareciendo disponible y recepción no se enteraba de los
turnos ya vendidos que caían dentro del bloqueo.

**Entregado**:

- `Mantenimiento` se apoya en una `Ocupacion` de tipo `MANTENIMIENTO` con `fechaHoraFin` nula:
  un bloqueo abierto se trata como infinito en `OcupacionRepository.existeSolapamiento`, así
  que ninguna reserva o entrenamiento nuevo entra en esa cancha hasta resolverlo.
- `POST /api/mantenimientos` abre el bloqueo y calcula las reservas vigentes afectadas;
  `PUT /{id}/resolver` recibe la solución aplicada, la guarda en el historial y libera la
  ocupación sin borrarla.
- Patrón **Observer**: `MantenimientoObserver` avisa a `ADMIN` y `RECEPCIONISTA` al abrir y al
  resolver (`NotificacionMantenimientoObserver`).
- UI: `/mantenimiento` para el rol `MANTENIMIENTO` (abrir y resolver), con consulta de solo
  lectura para admin y recepción. Muestra motivo, inicio, reservas afectadas, solución y estado.
- `ENCARGADO_MANTENIMIENTO` (23) no entra en `varchar(20)`: se mantiene `MANTENIMIENTO` (13).

## P8 — Diagramas y cierre de roles (pendiente)

Diagramas de clases y de casos de uso del sistema, con los cinco procesos y los seis roles
(`ADMIN`, `RECEPCIONISTA`, `UTILERIA`, `CAJA`, `MANTENIMIENTO`, `CLIENTE`).

## Trabajo fuera de plan

- **Cancelación por estado** (`feat(reservas)`, commit `bd6ec5b`): cancelar dejó de borrar la
  reserva para pasar a ser una transición de estado que conserva pagos y cuenta. Fue la primera
  rama real de la máquina de estados; el resto llegó con P3.
- **Registro y permisos del recepcionista** (`fix(usuarios)`, commit `65147c2`): alta rápida de
  `CLIENTE` desde el formulario de reserva y filtro JWT tolerante a tokens inválidos.
- **Separación de roles en la UI**: el `ADMIN` dejó de operar (historial de reservas de solo
  lectura, sin Utilería ni Caja); la barra lateral separa **Clientes** y **Personal**;
  el alta de personal elige el rol en el formulario y **Mi perfil** quedó al pie de la barra
  lateral. Se retiró el borrado destructivo de reservas e implementos (quedan Cancelar y
  Desactivar). El seed fija a Pedro como `RECEPCIONISTA`. La recepción dejó de operar Utilería
  (UI y backend) y Utilería no ve el módulo general de Reservas: solo su cola de turnos
  `EN_CURSO`.
