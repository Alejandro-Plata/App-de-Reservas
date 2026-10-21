# Reservas Station

Cliente web (Angular 20) para la **API de reservas** de `reservas-api` (Spring Boot + PostgreSQL + JWT), con el diseño inspirado en **Nintendo.com (2001)**: placas biseladas periwinkle, barra de mando carbon con enlaces dorados y rótulos con contorno.

## Funcionalidades
- Registro e inicio de sesión (JWT guardado en `localStorage`, guards por rol)
- Catálogo de recursos con búsqueda, paginación y categorías (Salas / Pistas / Equipos, desde la franja azul del header); los admins crean, editan y desactivan
- Detalle de recurso con disponibilidad por día y formulario de reserva (validación previa de fechas)
- Mis reservas paginadas y cancelación
- Panel admin con todas las reservas y filtros por estado, recurso y fechas
- Responsive: el raíl derecho baja bajo el contenido y las dos barras de navegación se funden en un menú

## Arranque
```bash
# 1. Levanta la API (en la carpeta de reservas-api)
docker compose up --build

# 2. Levanta este cliente
npm install
npm start          # http://localhost:4200
```
La URL de la API está en `src/app/services/auth-service.ts` (`API_URL`): `localhost:8080` en local y la API de Render en cualquier otro dominio.
Como el cliente corre en otro origen (4200), la API necesita **CORS** para `http://localhost:4200`.

## Despliegue
El frontend se publica en Vercel con el `vercel.json` incluido (salida `dist/reservas-web/browser`, con reescritura a `index.html` para las rutas). El dominio de Vercel tiene que estar en `CORS_ORIGINS` del backend. Demo: https://app-reservas-ten.vercel.app

## Contrato con la API
JSON que consume este cliente (documentación completa en el [Swagger de la API](https://reservas-api-nt3e.onrender.com/swagger-ui.html); los tipos están en `src/app/types`):

| Endpoint | Respuesta |
|---|---|
| `POST /auth/login`, `/auth/register` (`{email, password}`) | `{ token, email, role }` |
| `GET /resources?page&size&name&active&category` (`SALA`/`PISTA`/`EQUIPO`) | `Page<Resource>` de Spring (`content`, `totalPages`, `number`...) |
| `GET /resources/{id}/availability?date=yyyy-MM-dd` | `[{ startAt, endAt }]` con los huecos **ocupados** |
| `GET /reservations/me`, `GET /reservations?status&resourceId&from&to` | `Page<Reservation>` con `resourceName` y `userEmail` |
| Errores | `ProblemDetail` (`detail`) |

## Estructura
```
src/app
├── components/  header (doble barra), footer, paginación
├── pages/       login, register, recursos, recurso-detalle, mis-reservas, admin-reservas
├── services/    auth, recursos, reservas (fetch + token)
├── guards/      authGuard y adminGuard
├── types/       interfaces de la API
└── utils/       fechas
```
