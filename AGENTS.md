# AGENTS.md — Sistema Complejo Deportivo (monorepo)

Monorepo con dos subproyectos independientes que comparten un solo repositorio git.

## Estructura

| Carpeta | Qué es | Documentación |
|---|---|---|
| `backend/` | API REST — Spring Boot 4, Java 25, MySQL, JWT | `backend/AGENTS.md`, `backend/README.md` |
| `frontend/` | SPA — Angular 22 standalone, signals, Material | `frontend/README.md` |

Cada carpeta tiene **su propio `.gitignore`** y sus reglas aplican solo dentro de ella.
El `.gitignore` de la raíz cubre únicamente la raíz (van anclados con `/` a propósito).

El plan de trabajo por fases (P1…P8) y su estado están en `ROADMAP.md`.

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

- Roles: `ADMIN` ⊃ `RECEPCIONISTA` ⊃ `CLIENTE`, más los operativos `UTILERIA`, `CAJA` y
  `MANTENIMIENTO` (este último opera los bloqueos de cancha de P7). El JWT
  solo lleva el correo; el rol se lee de la base en cada request y el frontend lo obtiene de
  `GET /api/auth/me`.
- Seed: `admin@email.com` es `ADMIN`, `pedro@email.com` es `RECEPCIONISTA`, `utileria@email.com`
  es `UTILERIA`, `caja@email.com` es `CAJA` y `mantenimiento@email.com` es `MANTENIMIENTO`
  (clave `test123`).
- El `ADMIN` da de alta al personal desde **Personal** y elige el rol en el mismo formulario
  (la UI crea el `CLIENTE` con `POST /api/usuarios` y le asigna el rol con
  `PUT /api/usuarios/{id}/rol`); el selector no ofrece `CLIENTE`. En **Clientes** el rol no se
  edita.
- El `ADMIN` **gobierna, no opera**: en la UI no hace check-in/check-out, no entra a Utilería
  ni a Caja, no crea ni reprograma reservas. Su pantalla de Reservas es un **historial de solo
  lectura**. La matriz del backend sigue permitiéndolo como respaldo. En la barra lateral
  el admin tiene dos accesos, **Clientes** y **Personal**, y **Mi perfil** al pie.
- El `RECEPCIONISTA` también puede dar de alta y editar `CLIENTE` (registro presencial), incluso
  desde el formulario de reserva. No puede tocar al personal, cambiar roles ni borrar cuentas.
  La clave la entrega en mano el personal: no hay cambio forzado (riesgo aceptado).
- En clientes, el `ADMIN` ve a todos menos a sí mismo y el `RECEPCIONISTA` solo `CLIENTE`;
  nadie puede cambiar su propio rol ni borrar su cuenta.
- Un `CLIENTE` solo ve, reprograma y cancela **sus** reservas (reserva ajena → `403`).
- Cancelar es una transición de estado (`PUT /api/reservas/{id}/cancelar`): conserva la reserva
  y sus pagos, libera el turno y deja la cuenta `ANULADA`. El `DELETE` de reservas es solo
  `ADMIN` y es limpieza de datos, no una baja de servicio; **la UI ya no lo expone**.
- El check-in y el check-out los registra solo recepción: `PUT /api/reservas/{id}/check-in`
  (`CONFIRMADA → EN_CURSO`) y `.../check-out` (`EN_CURSO → COMPLETADA`). Los turnos que vencen
  sin check-in quedan `NO_ASISTIO`.
- La cancha y el horario viven en `Ocupacion`; `Reserva` solo la referencia.
- Los implementos son un catálogo (`Implemento`), **sin inventario ni stock**: el complejo
  administra las unidades por fuera. Al reservar se piden cantidades y quedan como líneas
  `IMPLEMENTO` con precio `0` (el préstamo es gratis).
- `UtileriaService` (rol `UTILERIA`; el backend admite `ADMIN` como respaldo, pero la UI no le
  da acceso) registra la entrega y la devolución como
  `MovimientoImplemento` (`ENTREGA` / `DEVOLUCION`: quién, cuándo, cuántas).
  Solo se opera sobre una reserva `EN_CURSO`.
- Al devolver, las unidades dañadas generan una línea `DANIO` y el faltante una `PERDIDA`.
  Ambas recalculan `Reserva.montoTotal` y `Cuenta.total`/`saldo`. El precio por defecto es
  `Implemento.valorReposicion`. También existe el cargo manual `DANIO` / `PERDIDA` / `SERVICIO`
  con `POST /api/reservas/{id}/cargos`.
- El orden del turno es: anticipo del 50 % → check-in → uso → devolución + cargos → caja cobra
  el saldo → check-out. `check-out` devuelve `409` si la cuenta tiene saldo; un `ADMIN` puede
  forzarlo con `?forzar=true` (solo por API; la UI no le expone check-out).
- Cada reserva nace con una `Cuenta` 1:1 y un `DetalleReserva` tipo pedido; `montoTotal` es la
  suma de las líneas. El anticipo del 50 % se registra al crear la reserva y el saldo se cobra
  después con `POST /api/pagos` (solo `ADMIN` / `CAJA`).
- `Pago` cuelga de la `Cuenta`, no de la reserva. Tipos: `ANTICIPO`, `SALDO`, `REEMBOLSO`.
  Daños y pérdidas son líneas del detalle, no pagos.
- Solapamiento de ocupaciones → `409`.
- Monto por hora iniciada: `horas = (minutos + 59) / 60`.
- `POST /api/usuarios` exige clave; `PUT /api/usuarios/{id}` la acepta opcional
  (vacía = no cambiar). El `RECEPCIONISTA` solo puede `PUT` sobre un `CLIENTE`.
- Un token JWT inválido no bloquea las rutas públicas: el filtro sigue como anónimo y la
  autorización de cada ruta decide (privadas → `401`).
- `POST /api/reservas` exige `metodoPago` (registra el anticipo del 50 %); `PUT /api/reservas/{id}`
  ignora `usuarioId`, `estado`, `montoTotal` y `implementos`, y también ignora el método de pago.
- `GET /api/reservas/{id}/cuenta` devuelve total, saldo, estado y pagos; un `CLIENTE` solo
  puede consultar la cuenta de sus propias reservas.
- `GET /api/reservas/{id}/implementos` devuelve pedidos, entregados, devueltos y pendientes por
  implemento; un `CLIENTE` solo puede consultar sus propias reservas.
- Los endpoints de utilería (`/{id}/implementos/**`, `/{id}/cargos`) son `POST` y exigen
  `ADMIN` / `UTILERIA`; la recepción no los opera. `GET /api/implementos` es para cualquier
  autenticado y el ABM del catálogo es solo `ADMIN`.
- **Entrenamientos (P6)**: `Entrenamiento` es 1:1 con una `Ocupacion` de tipo `ENTRENAMIENTO`;
  `InscripcionEntrenamiento` controla el cupo por cliente. `ADMIN` / `RECEPCIONISTA` crean,
  reprograman y cancelan; el `CLIENTE` se inscribe a sí mismo y libera su cupo. El `precio` es
  informativo: no genera `Cuenta` ni `Pago`.
- **Observer (P6/P7)**: cancelar o reprogramar un entrenamiento notifica a los inscriptos
  vigentes; abrir o resolver un mantenimiento notifica a `ADMIN` / `RECEPCIONISTA`. Las
  `Notificacion` se leen desde `/notificaciones` y la campana del layout.
- **Mantenimiento (P7)**: `Mantenimiento` es 1:1 con una `Ocupacion` de tipo `MANTENIMIENTO`
  sin fecha de fin mientras está abierto, así que bloquea la cancha por completo.
  `POST /api/mantenimientos` calcula las reservas afectadas y `PUT /{id}/resolver` recibe la
  solución aplicada y libera la cancha. Solo `MANTENIMIENTO` opera en la UI; el backend admite
  `ADMIN` como respaldo.

El detalle completo está en `backend/AGENTS.md`.

## Reglas de Eficiencia y Alcance (Ahorro de Turnos)
- **No ejecutes tests ni builds pesados por defecto:** No corras `.\mvnw.cmd test`, `ng test`, `npm run build` ni `.\mvnw.cmd clean` después de editar código, a menos que se solicite explícitamente.
- **No crees ni actualices archivos de test** (`*Tests.java`, `*.spec.ts`) al agregar o modificar vistas, estilos o endpoints.
- **Backend con Spring Boot DevTools:** Si necesitas aplicar un cambio en Java, basta con `.\mvnw.cmd compile` desde `backend/` (nunca `clean` si la app está corriendo en el puerto 8081).
- **Cambios visuales en Frontend:** Limítate a editar los archivos `.html`, `.css`/`.scss` y `.ts` estrictamente necesarios sin tocar contratos de API ni correr pruebas.
