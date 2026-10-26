# AGENTS.md — Sistema Complejo Deportivo (monorepo)

Monorepo con dos subproyectos independientes que comparten un solo repositorio git.

## Estructura

| Carpeta | Qué es | Documentación |
|---|---|---|
| `backend/` | API REST — Spring Boot 4, Java 25, MySQL, JWT | `backend/AGENTS.md`, `backend/README.md` |
| `frontend/` | SPA — Angular 22 standalone, signals, Material | `frontend/README.md` |

Cada carpeta tiene **su propio `.gitignore`** y sus reglas aplican solo dentro de ella.
El `.gitignore` de la raíz cubre únicamente la raíz (van anclados con `/` a propósito).

## Comandos

Todo se corre **desde la carpeta del subproyecto**:

```powershell
# backend
cd backend
.\mvnw.cmd clean compile      # build oficial
.\mvnw.cmd spring-boot:run    # API en http://localhost:8080
.\mvnw.cmd test               # tests (requiere MySQL arriba)

# frontend
cd frontend
npm install
npm start                     # SPA en http://localhost:4200
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
- Un `CLIENTE` solo ve, reprograma y cancela **sus** reservas (reserva ajena → `403`).
- Solapamiento de reservas → `409`.
- Monto por hora iniciada: `horas = (minutos + 59) / 60`.
- `POST /api/usuarios` exige clave; `PUT /api/usuarios/{id}` la acepta opcional
  (vacía = no cambiar).
- `PUT /api/reservas/{id}` ignora el `usuarioId` y nunca acepta `estadoReserva` ni
  `montoTotal`: los calcula el backend.

El detalle completo está en `backend/AGENTS.md`.
