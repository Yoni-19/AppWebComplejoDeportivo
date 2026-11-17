# Frontend — Sistema Complejo Deportivo

SPA del sistema de reservas. Angular standalone con signals y Angular Material.
Consume la API real del backend (`http://localhost:8081`): no hay datos en memoria.

## Requisitos

- Node.js 20+ con npm
- La API corriendo (`cd ../backend` → `.\mvnw.cmd spring-boot:run`)

## Cómo correr

```powershell
cd frontend
npm install
npm start        # dev server en http://localhost:4201
npm run build    # build de producción -> dist/
npm test         # tests (Vitest)
```

## Estructura

```
src/app/
├── core/          servicios, modelos, interceptores y guards compartidos
│   ├── api.constants.ts     API_BASE (apunta a http://localhost:8081)
│   ├── guards/              auth.guard (exige JWT en localStorage)
│   ├── http/api-error.ts    traduce el envelope de error del backend a un mensaje
│   ├── interceptors/        agrega Authorization: Bearer a cada request
│   ├── models/              tipos de dominio (Cancha, Cliente, Reserva, Cuenta, Pago,
│   │                        Entrenamiento, Mantenimiento, Notificación)
│   └── services/            auth.service.ts y notificacion.service.ts (bandeja/campana)
├── features/      un módulo por dominio: canchas/, clientes/, reservas/, pagos/, caja/,
│                  utileria/, implementos/, entrenamientos/, mantenimiento/,
│                  notificaciones/, dashboard/, login/, registro/
├── shared/        layout (menú lateral), componentes de UI y validadores
└── app.routes.ts  rutas + guards de autenticación y de rol
```

## Roles y pantallas

- `ADMIN`: canchas, implementos, usuarios (accesos **Clientes** y **Personal** en la barra
  lateral) e **historial** de reservas de solo lectura. En **Personal** da de alta personal
  eligiendo el rol y lo cambia después (sin `CLIENTE`); en **Clientes** no se edita el rol.
  No hace check-in/out ni entra a Utilería o Caja.
- `RECEPCIONISTA`: reservas, alta/edición de clientes, check-in/out y gestión de
  entrenamientos (alta, reprogramación y cancelación).
- `UTILERIA`: `/utileria` (solo turnos `EN_CURSO`; no ve el módulo general de Reservas).
  `CAJA`: `/caja` (cobro del saldo).
- `CLIENTE`: sus reservas, alta y reprogramación (no ve el estado al crear); en
  `/entrenamientos` consulta la lista, entra al detalle (`/entrenamientos/:id`) y se inscribe
  o libera su cupo.
- `MANTENIMIENTO`: `/mantenimiento`; abre bloqueos de cancha y los resuelve registrando la
  solución aplicada. El admin y la recepción ven el módulo en modo lectura, incluida la solución.
- Todos los roles tienen la campana de notificaciones en la barra superior y la bandeja
  `/notificaciones`.

## Convenciones

- Cada módulo tiene su `*.service.ts` (solo HTTP contra la API), sus componentes y sus modelos.
- Los formularios son reactivos con `FormBuilder`; cada uno maneja `loading` y `errorMessage`
  como signals y muestra el error del backend con `apiErrorMessage()`.
- Los formularios con confirmación de clave usan `ClavesIgualesErrorStateMatcher` para que el
  error del grupo se vea en el campo de repetición; el registro público además muestra un
  resumen cuando el formulario es inválido.
- La sesión vive en `localStorage`: `auth_token` (JWT) y `auth_correo`.

## Contratos que el frontend no puede cambiar

- `POST /api/usuarios` exige clave; `PUT /api/usuarios/{id}` la acepta opcional
  (vacía = no cambiar la actual).
- `PUT /api/reservas/{id}` ignora el `usuarioId` y nunca acepta `estado` ni
  `montoTotal`: los calcula el backend.
- `POST /api/reservas` exige `metodoPago`: el backend registra el anticipo del 50 % y devuelve
  la cuenta con su saldo. `PUT` ignora ese campo.
- `POST /api/pagos` solo acepta `monto` en `REEMBOLSO`; en `ANTICIPO` y `SALDO` el monto lo
  calcula el backend. Es una ruta solo para `ADMIN` y `CAJA`; en la UI cobra caja.
- `GET /api/reservas/{id}/cuenta` devuelve total, saldo, estado y movimientos.
- `GET /api/usuarios?rol=CLIENTE` es la variante para selectores y contadores. El admin no
  aparece en la lista de clientes (se edita desde "Mi perfil") y el recepcionista solo recibe
  `CLIENTE`.
- El `RECEPCIONISTA` puede crear (alta pública) y editar `CLIENTE` desde `/clientes/nueva`,
  `/clientes/{id}/editar` o el diálogo "Registrar cliente nuevo" de la reserva. No puede tocar
  personal, roles ni borrar cuentas. La clave se entrega en mano; no hay cambio forzado.
- El solapamiento de horarios devuelve `409`.
- La UI no ofrece borrado destructivo: las reservas se **cancelan** (transición de estado) y
  los implementos se **desactivan**.
- `POST /api/entrenamientos/{id}/inscripciones` no lleva cuerpo: el backend inscribe al usuario
  del token. `GET /api/entrenamientos` devuelve `inscriptos`, `cupoDisponible` e `inscripto`.
  El `precio` del entrenamiento es informativo y no genera pagos.
- `POST /api/mantenimientos` acepta `fechaHoraInicio` vacía (bloqueo desde ahora) y devuelve
  `reservasAfectadas`; `PUT /{id}/resolver` recibe `{ solucion }` obligatoria, libera la cancha
  y deja la solución visible en la lista. La UI del admin/recepción es de solo lectura.
- `GET /api/notificaciones` es la bandeja propia; `PUT /{id}/leer` y `PUT /leer-todas` marcan
  avisos. La campana usa `GET /api/notificaciones/no-leidas/count`.
