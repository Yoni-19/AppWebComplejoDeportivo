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
| 5 | Entrenamiento deportivo | A definir | pendiente |
| 6 | Mantenimiento de espacios | A definir | pendiente |

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
| P6 | Entrenamientos y patrón Observer | Pendiente |
| P7 | Mantenimiento de canchas | Pendiente |
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
- Frontend: botones de check-in y check-out en la lista, visibles solo para el personal.

## P4 — Utilería e implementos (implementado)

**Problema**: el préstamo de pelotas, redes y petos era invisible para el sistema. Cuando una
unidad se perdía o se dañaba, el monto no aparecía en la reserva y la caja cobraba de menos.

**Decisión de alcance**: el inventario físico lo administra el complejo por fuera; el sistema
**no** lleva stock. Solo define qué se presta (`Implemento`), qué pidió cada reserva, qué se
entregó, qué volvió y qué se cobra por lo que no volvió.

**Entregado**:

- Catálogo `Implemento` (`nombre`, `valorReposicion`, `activo`) con ABM en `/implementos`
  (`ADMIN`); todos los roles autenticados pueden consultarlo para elegir al reservar.
- Al crear la reserva se eligen implementos con cantidad: quedan como líneas `IMPLEMENTO` con
  precio `0` (el préstamo es gratis). El `PUT` las ignora.
- `MovimientoImplemento` registra la constancia del hecho físico: `ENTREGA` / `DEVOLUCION`,
  cantidad, fecha y quién lo registró. No es inventario, es trazabilidad.
- `GET /api/reservas/{id}/implementos` devuelve pedidos, entregados, devueltos y pendientes.
- `POST /api/reservas/{id}/implementos/entrega` y `.../devolucion`: utilería registra el
  movimiento. Al devolver, las unidades dañadas generan un cargo `DANIO` y el faltante un
  cargo `PERDIDA`; ambos recalculan el total de la reserva y el saldo de la cuenta.
- `POST /api/reservas/{id}/cargos`: cargo manual `DANIO` / `PERDIDA` / `SERVICIO`.
- Solo se opera sobre un turno `EN_CURSO`: antes del check-in no hay nada prestado y después
  del check-out ya se cerró.
- Frontend: pantalla `/utileria` con selector de turno en curso, entrega, devolución y cargo.

## P5 — Caja (implementado)

**Problema**: el importe a cobrar cambiaba después de los cargos de utilería y no había una
pantalla que mostrara "quién debe cuánto" ni que impidiera cerrar un turno con deuda.

**Entregado**:

- Pantalla `/caja`: reservas con cuenta `ABIERTA` y saldo pendiente, total por cobrar y cobro
  del saldo con el método elegido (`POST /api/pagos`, tipo `SALDO`). El monto lo calcula el
  backend, así los daños y pérdidas ya están incluidos.
- `PUT /api/reservas/{id}/check-out` rechaza el cierre con `409` si la cuenta tiene saldo.
  Un `ADMIN` puede forzarlo con `?forzar=true` (cliente que se retira sin pagar).
- Roles `CAJA` y `UTILERIA` agregados a la matriz de seguridad.

**Pendiente para cerrar del todo**: corte de caja diario (total por método y arqueo) y la
prueba manual del circuito completo con los usuarios del seed (`utileria@email.com`,
`caja@email.com`, clave `test123`).

## P6 — Entrenamientos y patrón Observer (pendiente)

Entrenamientos como `Ocupacion` de tipo propio, con inscripción de clientes. El Observer
notifica a los inscriptos cuando un entrenamiento se cancela o se reprograma.

**Fork abierto**: `Cuenta` hoy es 1:1 con `Reserva`. Un entrenamiento con N inscriptos no entra
en ese modelo: hay que decidir entre N cuentas (una por inscripto) o una cuenta del
entrenamiento con pagos por persona.

## P7 — Mantenimiento de canchas (pendiente)

Bloqueos de disponibilidad por mantenimiento, sin reserva asociada, y aviso al personal
cuando un bloqueo pisa turnos ya vendidos. El rol previsto es `ENCARGADO_MANTENIMIENTO`, que
**no** entra en `usuario.rol` (`varchar(20)`); si se agrega, usar un nombre más corto o ampliar
la columna.

## P8 — Diagramas y cierre de roles (pendiente)

Diagramas de clases y de casos de uso del sistema, con los cinco procesos y los cinco roles
(`ADMIN`, `RECEPCIONISTA`, `UTILERIA`, `CAJA`, `CLIENTE`).

## Trabajo fuera de plan

- **Cancelación por estado** (`feat(reservas)`, commit `bd6ec5b`): cancelar dejó de borrar la
  reserva para pasar a ser una transición de estado que conserva pagos y cuenta. Fue la primera
  rama real de la máquina de estados; el resto llegó con P3.
- **Registro y permisos del recepcionista** (`fix(usuarios)`, commit `65147c2`): alta rápida de
  `CLIENTE` desde el formulario de reserva y filtro JWT tolerante a tokens inválidos.
