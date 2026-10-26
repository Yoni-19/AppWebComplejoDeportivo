# Frontend — Sistema Complejo Deportivo

SPA del sistema de reservas. Angular standalone con signals y Angular Material.
Consume la API real del backend (`http://localhost:8080`): no hay datos en memoria.

## Requisitos

- Node.js 20+ con npm
- La API corriendo (`cd ../backend` → `.\mvnw.cmd spring-boot:run`)

## Cómo correr

```powershell
cd frontend
npm install
npm start        # dev server en http://localhost:4200
npm run build    # build de producción -> dist/
npm test         # tests (Vitest)
```

## Estructura

```
src/app/
├── core/          servicios, modelos, interceptores y guards compartidos
│   ├── api.constants.ts     API_BASE (apunta a http://localhost:8080)
│   ├── guards/              auth.guard (exige JWT en localStorage)
│   ├── http/api-error.ts    traduce el envelope de error del backend a un mensaje
│   ├── interceptors/        agrega Authorization: Bearer a cada request
│   ├── models/              tipos de dominio (Cancha, Cliente, Reserva)
│   └── services/auth.service.ts
├── features/      un módulo por dominio: canchas/, clientes/, reservas/, dashboard/, login/
├── shared/        layout (menú lateral) y componentes de UI
└── app.routes.ts  rutas + guard de autenticación
```

## Convenciones

- Cada módulo tiene su `*.service.ts` (solo HTTP contra la API), sus componentes y sus modelos.
- Los formularios son reactivos con `FormBuilder`; cada uno maneja `loading` y `errorMessage`
  como signals y muestra el error del backend con `apiErrorMessage()`.
- La sesión vive en `localStorage`: `auth_token` (JWT) y `auth_correo`.

## Contratos que el frontend no puede cambiar

- `POST /api/usuarios` exige clave; `PUT /api/usuarios/{id}` la acepta opcional
  (vacía = no cambiar la actual).
- `PUT /api/reservas/{id}` ignora el `usuarioId` y nunca acepta `estadoReserva` ni
  `montoTotal`: los calcula el backend.
- `POST /api/pagos` no acepta `monto`: el servicio lo fuerza al total de la reserva.
- La solapamiento de horarios devuelve `409`.
