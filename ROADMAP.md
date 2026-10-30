# ROADMAP — Sistema Complejo Deportivo

Estado de los procesos del sistema, en el orden en que se están construyendo.
Cada fase se cierra con los tests del backend en verde, el build del frontend OK y la
documentación (`AGENTS.md`, `README.md` de cada subproyecto) alineada con el código.

| Fase | Proceso | Estado |
|---|---|---|
| P1 | Calendario único de canchas | Cerrado |
| P2 | Cobros | Cerrado |
| P3 | Check-in / check-out | Implementado (falta prueba manual) |
| P4 | Caja | Siguiente |
| P5 | Entrenamientos y patrón Observer | Pendiente |
| P6 | Mantenimiento de canchas | Pendiente |
| P7 | Roles nuevos y diagramas | Pendiente |

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

## P3 — Check-in / check-out (implementado)

**Problema**: `EstadoServicio` declaraba `EN_CURSO`, `COMPLETADA` y `NO_ASISTIO`, pero ninguna
transición del código los producía. Solo el seed los usaba, así que esos estados eran
decorativos y las reglas que dependen de ellos (no reprogramar, no cancelar) no protegían nada.

**Entregado**:

- `PUT /api/reservas/{id}/check-in`: `CONFIRMADA → EN_CURSO`. Solo personal, desde una hora
  antes del inicio y hasta el fin del turno.
- `PUT /api/reservas/{id}/check-out`: `EN_CURSO → COMPLETADA`. Solo personal.
- `NO_ASISTIO`: los turnos confirmados cuyo horario ya terminó se vencen al listar reservas
  (`ReservaService.listarTodos`), sin scheduler.
- No se reprograma una reserva `EN_CURSO` (`409`).
- Frontend: botones de check-in y check-out en la lista, visibles solo para el personal.

**Decisión tomada**: el check-out **no** cobra el saldo. Cambiar de estado es una operación de
cancha; cobrar es de caja (`POST /api/pagos`). La cuenta se cierra sola cuando el saldo llega a
`0`. Si el negocio exige "no hay check-out sin pagar", se agrega como guard en
`ReservaService.registrarCheckOut`.

## P4 — Caja (pendiente)

Cierre diario de los movimientos del complejo: total cobrado por tipo de pago, saldos
pendientes y arqueo contra lo registrado en `Pago`.

## P5 — Entrenamientos y patrón Observer (pendiente)

Entrenamientos como `Ocupacion` de tipo propio, con inscripción de clientes. El Observer
notifica a los inscriptos cuando un entrenamiento se cancela o se reprograma.

## P6 — Mantenimiento de canchas (pendiente)

Bloqueos de disponibilidad por mantenimiento, sin reserva asociada, y aviso al personal
cuando un bloqueo pisa turnos ya vendidos.

## P7 — Roles nuevos y diagramas (pendiente)

Roles adicionales al trío `ADMIN` / `RECEPCIONISTA` / `CLIENTE` (por ejemplo, profesor de
entrenamientos) y los diagramas de clases y de casos de uso del sistema.

## Trabajo fuera de plan

- **Cancelación por estado** (`feat(reservas)`, commit `bd6ec5b`): cancelar dejó de borrar la
  reserva para pasar a ser una transición de estado que conserva pagos y cuenta. Fue la primera
  rama real de la máquina de estados; el resto llegó con P3.
- **Registro y permisos del recepcionista** (`fix(usuarios)`, commit `65147c2`): alta rápida de
  `CLIENTE` desde el formulario de reserva y filtro JWT tolerante a tokens inválidos.
