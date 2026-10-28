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
│   ├── models/              tipos de dominio (Cancha, Cliente, Reserva, Cuenta, Pago)
│   └── services/auth.service.ts
├── features/      un módulo por dominio: canchas/, clientes/, reservas/, pagos/, dashboard/, login/
├── shared/        layout (menú lateral), componentes de UI y validadores
└── app.routes.ts  rutas + guard de autenticación
```

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
  calcula el backend. Es una ruta solo para `ADMIN` y `RECEPCIONISTA`.
- `GET /api/reservas/{id}/cuenta` devuelve total, saldo, estado y movimientos.
- `GET /api/usuarios?rol=CLIENTE` es la variante para selectores y contadores. El admin no
  aparece en la lista de clientes (se edita desde "Mi perfil") y el recepcionista solo recibe
  `CLIENTE`.
- El `RECEPCIONISTA` puede crear (alta pública) y editar `CLIENTE` desde `/clientes/nueva`,
  `/clientes/{id}/editar` o el diálogo "Registrar cliente nuevo" de la reserva. No puede tocar
  personal, roles ni borrar cuentas. La clave se entrega en mano; no hay cambio forzado.
- El solapamiento de horarios devuelve `409`.
