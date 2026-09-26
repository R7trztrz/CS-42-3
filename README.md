# CS-42-3

## Docker quick start

This project can be started with Docker Compose. The Docker setup starts three
services:

- `frontend`: React/Vite frontend on `http://localhost:5173`
- `backend`: Spring Boot backend on `http://localhost:8080`
- `db`: PostgreSQL database on `localhost:5432`

Before running Docker, copy the example Docker environment file:

```bash
cp .env.docker.example .env
```

Update `.env` if real local secrets are required, especially the Turnstile
values.

Start the full project:

```bash
docker compose up --build
```

Stop the project:

```bash
docker compose down
```
