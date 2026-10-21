# Reservas

Aplicación full-stack para **reservar recursos** (salas, pistas deportivas, equipos) sin dobles reservas, con autenticación por roles. Está formada por dos proyectos que viven en este mismo repositorio:

| Carpeta | Qué es | Tecnologías |
|---|---|---|
| [`reservas-api`](./reservas-api) | API REST (backend) | Java 21, Spring Boot 3, Spring Security + JWT, JPA, PostgreSQL, Flyway, Testcontainers, Docker |
| [`reservas-web`](./reservas-web) | Cliente web (frontend), diseño estilo Nintendo.com 2001 | Angular 20, TypeScript, Reactive Forms |

```
┌────────────────┐   HTTP + JWT   ┌────────────────┐   JDBC   ┌──────────────┐
│  reservas-web  │ ─────────────► │  reservas-api  │ ───────► │  PostgreSQL  │
│  Angular :4200 │ ◄───────────── │  Spring :8080  │          │  (Flyway)    │
└────────────────┘     JSON       └────────────────┘          └──────────────┘
```

## Qué hace
- **Usuarios:** se registran, inician sesión, consultan la disponibilidad de un recurso por día, reservan y cancelan sus reservas.
- **Administradores:** crean, editan y desactivan recursos, y ven y filtran las reservas de todos (por estado, recurso y fechas).
- **Sin solapamientos:** dos reservas confirmadas del mismo recurso no pueden coincidir en el tiempo. PostgreSQL lo impone con una restricción del tipo `EXCLUDE USING gist`. 

## Arranque rápido (local)
Docker y Node.js 20+.

```bash
cd reservas-api
docker compose up --build

cd reservas-web
npm install
npm start
```

Administrador inicial solo para desarrollo: `admin@reservas.local` / `admin-local-1234`.

## Tests
```bash
cd reservas-api && ./mvnw verify     
cd reservas-web && npm run build
```
Ambos se ejecutan en GitHub Actions (`.github/workflows/ci.yml`) en cada push y pull request.

## Despliegue
| Pieza | Plataforma | Configuración |
|---|---|---|
| API | Render (Docker) | [`render.yaml`](./render.yaml) |
| Base de datos | Neon (PostgreSQL) | variables `DB_URL`, `DB_USER`, `DB_PASSWORD` |
| Web | Vercel | [`reservas-web/vercel.json`](./reservas-web/vercel.json) |

## Documentación de cada parte
- [`reservas-api/README.md`](./reservas-api/README.md): endpoints, variables de entorno y decisiones de diseño.
- [`reservas-web/README.md`](./reservas-web/README.md): funcionalidades, estructura y contrato con la API.
