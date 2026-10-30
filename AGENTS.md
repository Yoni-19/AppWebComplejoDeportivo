# AGENTS.md — Sistema Complejo Deportivo (monorepo)

Monorepo con dos subproyectos independientes que comparten un solo repositorio git.

## Estructura

| Carpeta | Qué es | Documentación |
|---|---|---|
| `backend/` | API REST — Spring Boot 4, Java 25, MySQL, JWT | `backend/AGENTS.md`, `backend/README.md` |
| `frontend/` | SPA — Angular 22 standalone, signals, Material | `frontend/README.md` |

Cada carpeta tiene **su propio `.gitignore`** y sus reglas aplican solo dentro de ella.
El `.gitignore` de la raíz cubre únicamente la raíz (van anclados con `/` a propósito).

El plan de trabajo por fases (P1…P7) y su estado están en `ROADMAP.md`.

## Comandos

Todo se corre **desde la carpeta del subproyecto**:

```powershell
# backend
cd backend
.\mvnw.cmd clean compile      # build oficial
.\mvnw.cmd spring-boot:run    # API en http://localhost:8081
.\mvnw.cmd test               # tests (requiere MySQL arriba)

# frontend
cd frontend
npm install
npm start                     # SPA en http://localhost:4201
npm run build                 # build de producción
```

## VS Code

El workspace se abre en **la raíz**. VS Code solo lee el `.vscode/` de la raíz, así que ahí
están la configuración de Java (`settings.json`) y las tareas de Angular (`tasks.json`,
`launch.json`). Si abrís `backend/` como carpeta independiente, su `.vscode/settings.json`
propio sigue existiendo y sigue funcionando.

## Convenciones

- Idioma del código, los comentarios y la documentación: **español**. Los identificadores del
  dominio (`Cancha`, `Reserva`, `Pago`, `Usuario`) van en camelCase; los endpoints en inglés
  plural (`/api/canchas`, `/api/reservas`, `/api/pagos`, `/api/usuarios`).
- Commits en castellano con formato `tipo(ámbito): descripción`
  (`feat`, `fix`, `refactor`, `docs`, `chore`). Sin atribución de IA.
- Un commit por unidad de trabajo revisable: cada commit compila y se puede explicar en una línea.
- Nunca serializar entidades JPA: todo sale por DTOs.
- El frontend consume la API real: los servicios usan `HttpClient`, no datos en memoria.

## Reglas de negocio (resumen)

- Roles: `ADMIN` ⊃ `RECEPCIONISTA` ⊃ `CLIENTE`. El JWT solo lleva el correo; el rol se lee de
  la base en cada request y el frontend lo obtiene de `GET /api/auth/me`.
- El registro público (`POST /api/usuarios`) crea siempre un `CLIENTE`.
- El `RECEPCIONISTA` también puede dar de alta y editar `CLIENTE` (registro presencial), incluso
  desde el formulario de reserva. No puede tocar al personal, cambiar roles ni borrar cuentas.
  La clave la entrega en mano el personal: no hay cambio forzado (riesgo aceptado).
- En clientes, el `ADMIN` ve a todos menos a sí mismo y el `RECEPCIONISTA` solo `CLIENTE`;
  nadie puede cambiar su propio rol ni borrar su cuenta.
- Un `CLIENTE` solo ve, reprograma y cancela **sus** reservas (reserva ajena → `403`).
- Cancelar es una transición de estado (`PUT /api/reservas/{id}/cancelar`): conserva la reserva
  y sus pagos, libera el turno y deja la cuenta `ANULADA`. El `DELETE` de reservas es solo
  `ADMIN` y es limpieza de datos, no una baja de servicio.
- El check-in y el check-out los registra solo el personal: `PUT /api/reservas/{id}/check-in`
  (`CONFIRMADA → EN_CURSO`) y `.../check-out` (`EN_CURSO → COMPLETADA`). Los turnos que vencen
  sin check-in quedan `NO_ASISTIO`.
- La cancha y el horario viven en `Ocupacion`; `Reserva` solo la referencia.
- Cada reserva nace con una `Cuenta` 1:1 y un `DetalleReserva` tipo pedido; `montoTotal` es la
  suma de las líneas. El anticipo del 50 % se registra al crear la reserva y el saldo se cobra
  después con `POST /api/pagos` (solo `ADMIN` / `RECEPCIONISTA`).
- `Pago` cuelga de la `Cuenta`, no de la reserva. Tipos: `ANTICIPO`, `SALDO`, `REEMBOLSO`.
  Daños y pérdidas son líneas del detalle, no pagos.
- Solapamiento de ocupaciones → `409`.
- Monto por hora iniciada: `horas = (minutos + 59) / 60`.
- `POST /api/usuarios` exige clave; `PUT /api/usuarios/{id}` la acepta opcional
  (vacía = no cambiar). El `RECEPCIONISTA` solo puede `PUT` sobre un `CLIENTE`.
- Un token JWT inválido no bloquea las rutas públicas: el filtro sigue como anónimo y la
  autorización de cada ruta decide (privadas → `401`).
- `POST /api/reservas` exige `metodoPago` (registra el anticipo del 50 %); `PUT /api/reservas/{id}`
  ignora `usuarioId`, `estado` y `montoTotal`, y también ignora el método de pago.
- `GET /api/reservas/{id}/cuenta` devuelve total, saldo, estado y movimientos; un `CLIENTE`
  solo puede consultar sus propias reservas.

El detalle completo está en `backend/AGENTS.md`.
