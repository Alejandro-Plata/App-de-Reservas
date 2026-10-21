# Reservas API

API REST para gestionar reservas de recursos (salas, pistas...) con control de solapamientos garantizado por la base de datos, autenticación JWT y roles `USER` / `ADMIN`.

## Tecnologías
Java 21 · Spring Boot 3 · Spring Security (JWT) · Spring Data JPA · PostgreSQL · Flyway · Testcontainers · Docker · GitHub Actions

## Funcionalidades
- Registro e inicio de sesión con JWT (contraseñas con BCrypt)
- Gestión de recursos: solo administradores crean, editan y desactivan (nunca se borran)
- Creación y cancelación de reservas sin solapamientos; cancelar libera el hueco
- Disponibilidad de un recurso por día
- Listados paginados (máximo 50 por página) con filtros por estado, recurso y fechas
- Errores en formato `ProblemDetail` (RFC 9457) y documentación en Swagger UI

## Arranque rápido
```bash
cp .env.example .env      # opcional: el compose trae valores de desarrollo por defecto
docker compose up --build
```
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Administrador inicial (solo desarrollo): `admin@reservas.local` / `admin-local-1234` (variables `ADMIN_EMAIL` y `ADMIN_PASSWORD`)

Para arrancar desde el IDE, levanta solo la base de datos (`docker compose up db`) y exporta `JWT_SECRET` (mínimo 32 caracteres).

### Variables de entorno
| Variable | Para qué | Por defecto |
|---|---|---|
| `JWT_SECRET` | Firma de tokens (≥ 32 caracteres, **obligatoria**) | – |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL | localhost / `reservas` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Crea un admin al arrancar si no existe | vacío (no crea) |
| `CORS_ORIGINS` | Orígenes del frontend permitidos | `localhost:4200` y `127.0.0.1:4200` |
| `APP_TIME_ZONE` | Zona con la que se interpreta "un día" | `Europe/Madrid` |

## Ejecutar los tests
```bash
mvn verify      # requiere Docker (Testcontainers)
```

## Endpoints
| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | público |
| GET | `/api/resources` (`name`, `active`, `category`, `page`, `size`) | autenticado |
| GET | `/api/resources/{id}`, `/api/resources/{id}/availability?date=yyyy-MM-dd` | autenticado |
| POST / PUT / DELETE | `/api/resources`, `/api/resources/{id}` | ADMIN (DELETE desactiva) |
| POST | `/api/reservations` | autenticado |
| GET | `/api/reservations/me` | autenticado |
| GET | `/api/reservations` (`status`, `resourceId`, `from`, `to`) | ADMIN |
| PATCH | `/api/reservations/{id}/cancel` | dueño o ADMIN |

Códigos: `400` validación · `401` sin token · `403` sin permiso · `404` no existe · `409` hueco ocupado / ya cancelada / e-mail repetido · `422` regla de negocio (pasado, duración, recurso inactivo).

Cliente web: [`reservas-web`](../reservas-web) (Angular). Visión global del proyecto en el [README principal](../README.md).

## Decisiones de diseño
- **Paquetes por funcionalidad** (`user`, `resource`, `reservation`) en vez de por capa técnica: cada feature se entiende y se modifica en un solo sitio. Controller → Service → Repository; el controlador nunca toca el repositorio y no se exponen entidades JPA (DTOs como `record`).
- **Sin doble reservado, en dos niveles.** El servicio comprueba el solapamiento (`start < otroFin AND end > otroInicio`) para dar un 409 claro; pero entre esa comprobación y el `INSERT` otra petición podría colarse, así que la garantía real es una restricción `EXCLUDE USING gist` de PostgreSQL sobre `tstzrange(start_at, end_at)` solo para reservas `CONFIRMED`. Si salta, se traduce a 409. Un test lanza 10 peticiones simultáneas al mismo hueco y exactamente una se confirma. Los tramos son `[inicio, fin)`: una reserva puede empezar justo cuando acaba otra.
- **Fechas** como `Instant` / `timestamptz`: un instante absoluto, sin ambigüedad de zona. La zona (`APP_TIME_ZONE`) solo se usa para decidir qué es "un día" en disponibilidad y filtros.
- **Errores**: un único `@RestControllerAdvice` produce `ProblemDetail` con `timestamp` (y `errors` por campo en validación); los 401/403 del filtro de seguridad usan el mismo formato. El cliente nunca recibe una traza.
- **Seguridad**: JWT sin estado firmado con HS256; el rol viaja en el token, así que un cambio de rol se aplica al volver a iniciar sesión. Reglas de acceso por ruta en `SecurityConfig`. Se eligió `jjwt` frente a `oauth2-resource-server` por simplicidad: la propia API emite los tokens.
- **Reglas de duración**: mínimo 30 min y máximo 4 h (`app.reservation.*` en `application.yml`).
- **Flyway** gestiona el esquema y Hibernate solo lo valida (`ddl-auto: validate`).

## Mejoras futuras
- Refresh tokens y logout (revocación): hoy un token robado vale hasta que caduca (60 min).
- Rate limiting en el login, caché de disponibilidad y notificaciones por email.
- Perfiles `dev` / `prod` y métricas con Actuator.

## Estructura
```
src/main/java/com/alejandroplata/reservasapi
├── config/            configuración (OpenAPI, propiedades...)
├── security/          JWT y reglas de acceso
├── common/exception/  errores y manejo global
├── user/              usuarios y autenticación
├── resource/          recursos reservables
└── reservation/       reservas y reglas de negocio
```
