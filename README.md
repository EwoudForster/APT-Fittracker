# FitnessTracker Microservices – Full Fixed Starter

This is a complete, compile-ready starter with 3 microservices + API Gateway:

- `users-service` (Spring Boot, JPA, PostgreSQL)
- `workouts-service` (Spring Boot, MongoDB)
- `progress-service` (Spring Boot, JPA, PostgreSQL)
- `gateway` (Spring Cloud Gateway)
- `docker-compose.yml` for Postgres, Mongo and all services

## Quick start

```bash
# From repo root
docker compose build --no-cache
docker compose up -d
```

Test through the Gateway (port 8080):
- `GET http://localhost:8080/api/users`
- `POST http://localhost:8080/api/users`
  ```json
  {"email":"you@example.com", "displayName":"You"}
  ```

### Local (without Docker)
Build each module:
```bash
cd users-service && mvn -q -U -DskipTests package && cd ..
cd workouts-service && mvn -q -U -DskipTests package && cd ..
cd progress-service && mvn -q -U -DskipTests package && cd ..
cd gateway && mvn -q -U -DskipTests package && cd ..
```
Run each service (ports: 8081, 8082, 8083, gateway 8080).

## Notes
- Spring Boot 3.3.x + Java 21.
- Spring Cloud BOM 2024.0.2 in the Gateway POM.
- UUIDs are generated in `@PrePersist` to avoid DB-specific generators.
