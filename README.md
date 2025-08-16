# APT-Fittracker
Advanced Programming Topics - Fittracker

# FitTrackr — Microservice backend (scoregericht ontwerp)

> Thema: **fitness & coaching** — gebruikers loggen workouts, volgen voortgang, plannen schema’s en verdienen badges. **Geen orders/inventory**.

Dit document mapt 1-op-1 op de evaluatie-eisen en geeft je een klaar-om-te-bouwen architectuur met endpoints, databanken, deployment, security, testing, CI/CD en optionele uitbreidingen om extra punten te scoren.

---

## 0. Overzicht & componenten

**Core (4 microservices)**

1. **Users Service** (SQL/PostgreSQL) — accounts, profielen, coach/klant-relatie.
2. **Workouts Service** (MongoDB) — workout templates, logs, PR’s (records).
3. **Nutrition Service** (PostgreSQL) — maaltijden, macro’s, dagsamenvattingen.
4. **Achievements Service** (MongoDB, Kafka consumer)\*\* — badges/levels op basis van events.

**Infra & rand**

* **API Gateway**: Spring Cloud Gateway (Java)
* **Auth**: GCP OAuth2 via Google (basis) of eigen Auth-service (optie 2.3)
* **Kafka** (+ Zookeeper) voor event-driven interacties (optie 2.5 / 2.5.1)
* **Redis** voor rate limiting (optie 2.4)
* **Prometheus + Grafana** (opties 2.2.1/2.2.1.1)

**Solo?** Maak 3 services: **Users, Workouts, Achievements** (Nutrition optioneel).
**Met 2?** Gebruik alle 4 core services (vereiste gehaald).

**Taal & Stack**

* Java 21, Spring Boot 3.3, Spring Cloud 2024.x
* Spring Data JPA (Postgres), Spring Data MongoDB, Spring Security OAuth2
* Test: JUnit 5, Mockito, Testcontainers
* CI/CD: GitHub Actions (build, test, docker build/push)

---

## 1. Algemene eisen & documentatie (≥60%)

### 1.1 Endpoints (via API Gateway)

> **Minstens 3×GET, 1×POST, 1×PUT, 1×DELETE** — hieronder staat een compacte set die sowieso via de Gateway exposed wordt.

**Users Service (SQL)**

* `GET /api/users/me` → eigen profiel (GET #1)
* `GET /api/users/{id}` → publiek profiel (GET #2)
* `PUT /api/users/me` → profiel updaten (PUT #1)
* `DELETE /api/users/me` → account verwijderen (DELETE #1)

**Workouts Service (Mongo)**

* `GET /api/workouts/templates?muscle=legs&difficulty=EASY` (GET #3, met **query params**)
* `POST /api/workouts/logs` → log aanmaken (POST #1, **body**)
* `GET /api/workouts/logs?from=2025-08-01&to=2025-08-31` (GET #4)

**Achievements Service (Mongo, event-driven)**

* `GET /api/achievements/me` (GET #5)

**Nutrition Service (SQL)**

* `GET /api/nutrition/day?date=2025-08-16` (GET #6)

> **Path params**: bv. `/api/users/{id}`.
> **Query params**: bv. `?muscle=legs`, `?from`, `?to`, `?date`.
> **Body**: bv. POST `/workouts/logs` met JSON payload.

#### Voorbeeld payloads

**POST /api/workouts/logs**

```json
{
  "workoutDate": "2025-08-16",
  "exercises": [
    { "name": "Bench Press", "sets": [ {"reps": 8, "weight": 60}, {"reps": 6, "weight": 65} ] },
    { "name": "Pull-ups", "sets": [ {"reps": 5}, {"reps": 4} ] }
  ],
  "durationMin": 55,
  "notes": "Voelde goed"
}
```

**PUT /api/users/me**

```json
{
  "displayName": "Ewoud",
  "heightCm": 183,
  "goal": "cut"
}
```

### 1.2 Databanken

* **PostgreSQL** (SQL): **Users** en **Nutrition**

  * Tabel `users(id, email UNIQUE, display_name, height_cm, goal, created_at)`
  * Tabel `daily_nutrition(id, user_id FK→users.id, date, calories, protein_g, carbs_g, fat_g)`
* **MongoDB** (document): **Workouts** en **Achievements**

  * Collection `workout_logs { _id, userId, workoutDate, exercises[], durationMin, notes }`
  * Collection `achievements { _id, userId, badges: [ {code, earnedAt} ], level }`

**Keuzes motiveren**

* **Mongo** voor onregelmatige, geneste workout-logs.
* **SQL** voor goed genormaliseerde user- en nutrition-data, relaties en rapportering.

### 1.3 Documentatie (README)

* **Thema** + korte uitleg waarvoor de backend gebruikt wordt.
* **Lijst & schema** van microservices, gateway, message broker, DB’s.
* **Links** naar hosted artefacts (Docker Hub images, GH Actions)
* **Postman screenshots** van alle endpoints (GET/POST/PUT/DELETE)
* **Deployment** uitleg (Compose/K8s) + security en testing secties.

### 1.4 Deployment

* **Elke component** heeft een **Dockerfile** (multi-stage: build + runtime).
* **GitHub Actions** build images **per service** bij push/PR.
* **`docker-compose.yml`** start DB’s, broker en services lokaal.

### 1.5 Security

* **GCP OAuth2 op de Gateway** met Google (secured/unsecured routes)

  * Unsecured: health, docs.
  * Secured: alles onder `/api/**` (behalve bv. `/api/public/**`).

### 1.6 Testing

* **Unit tests** voor **alle Service-klassen** (businesslogica)
* Gebruik **Mockito** + **Testcontainers** voor DB-interacties.

---

## 2. SUGGESTIES VOOR AANVULLINGEN (extra punten)

### 2.1 Front-end (in container) **(+15%)**

* React + Vite + TypeScript: login via Gateway, schermen: Dashboard, Workouts, Achievements, Nutrition.
* Containeriseer met Nginx of Node (serve) en koppel via compose/k8s.

### 2.2 Kubernetes manifests **(+5%)**

* Maak `k8s/` met `Deployment`, `Service` per component.
* **2.2.2** Gebruik **ClusterIP** intern en **NodePort** voor de Gateway **(+5%)**

### 2.2.1 Monitoring met Prometheus **(+20%)**

* Expose `/actuator/prometheus` in services, scrape met Prometheus.
* **2.2.1.1 Grafana** dashboards voor Gateway latency, request rate **(+15%)**

### 2.3 Eigen Auth-service (i.p.v. GCP) **(+25%)**

* OpenID Connect provider (Keycloak) of self-built JWT-issuer; Gateway valideert tokens.

### 2.4 Rate limiting op Gateway **(+5%)**

* Redis-backed `RequestRateLimiter` filter per route (bijv. 20 req/min per user).

### 2.5 Event-driven async **(+20%)**

* **Workouts Service** publiceert `workouts.logged.v1` naar Kafka.
* **Achievements Service** consumeert en kent badges toe.
* **2.5.1 Kafka (i.p.v. ActiveMQ)** **(+15%)**

---

## 3. Architectuur & deployment schema

```
[ Web/Front-end ]
       │   (OAuth2 login)
       ▼
[ Spring Cloud Gateway ]───►[ Redis ] (rate limit)
   │        │                  
   │        ├──secured /api/** (requires OAuth)
   │        └──unsecured /actuator/**, /public/**
   │
   ├──► Users Service (PostgreSQL)
   ├──► Workouts Service (MongoDB)
   ├──► Nutrition Service (PostgreSQL)
   └──► Achievements Service (MongoDB, Kafka consumer)
                      ▲
                      │ (events: workouts.logged.v1)
            [ Kafka + Zookeeper ]

[ Prometheus ]◄──scrape── /actuator/prometheus (alle services)
[ Grafana ]  ◄──datasource: Prometheus
```

---

## 4. API Gateway configuratie (Spring Cloud Gateway)

`application.yml` (uittreksel):

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
        provider:
          google:
            issuer-uri: https://accounts.google.com
  cloud:
    gateway:
      default-filters:
        - TokenRelay
      routes:
        - id: users
          uri: http://users:8080
          predicates:
            - Path=/api/users/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
        - id: workouts
          uri: http://workouts:8080
          predicates:
            - Path=/api/workouts/**
        - id: nutrition
          uri: http://nutrition:8080
          predicates:
            - Path=/api/nutrition/**
        - id: achievements
          uri: http://achievements:8080
          predicates:
            - Path=/api/achievements/**

# Beveilig openbare endpoints expliciet in WebSecurityConfig (permitAll)
```

`WebSecurityConfig.java` (schets):

```java
http
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**", "/public/**").permitAll()
    .anyRequest().authenticated()
  )
  .oauth2Login(Customizer.withDefaults())
  .oauth2ResourceServer(oauth2 -> oauth2.jwt());
```

---

## 5. Service ontwerpen

### 5.1 Users Service (PostgreSQL)

**Entity**

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  display_name TEXT,
  height_cm INT,
  goal TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);
```

**Endpoints**

* `GET /api/users/me` (haalt info uit JWT `sub`/email)
* `GET /api/users/{id}`
* `PUT /api/users/me`
* `DELETE /api/users/me`

**Service testideeën**

* Email-unique validatie, update merge logica, soft-delete vs hard-delete (kies één).

### 5.2 Workouts Service (MongoDB)

**Document**

```json
{
  "_id": "...",
  "userId": "...",
  "workoutDate": "2025-08-16",
  "exercises": [ {"name":"Bench Press","sets":[{"reps":8,"weight":60}] } ],
  "durationMin": 55,
  "notes": "..."
}
```

**Endpoints**

* `POST /api/workouts/logs`
* `GET /api/workouts/logs?from&to`
* `GET /api/workouts/templates?muscle&difficulty`

**Events**

* Na `POST logs` → produceer `workouts.logged.v1` (key=userId).

### 5.3 Nutrition Service (PostgreSQL)

**Tables**

```sql
CREATE TABLE daily_nutrition (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  date DATE NOT NULL,
  calories INT,
  protein_g INT,
  carbs_g INT,
  fat_g INT,
  UNIQUE(user_id, date)
);
```

**Endpoints**

* `GET /api/nutrition/day?date=`
* `PUT /api/nutrition/day` (upsert macro’s voor datum)

### 5.4 Achievements Service (MongoDB, Kafka)

**Logica**

* Consume `workouts.logged.v1`.
* Regels: 1e log → badge `FIRST_WORKOUT`; 5 logs in 7 dagen → `STREAK_5`; totaal volume > X → `VOLUME_BRONZE`.

**Endpoints**

* `GET /api/achievements/me`

---

## 6. Docker Compose (dev)

`docker-compose.yml` (basis)

```yaml
version: "3.9"
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: fittrackr
    ports: ["5432:5432"]
  mongo:
    image: mongo:7
    ports: ["27017:27017"]

  zookeeper:
    image: bitnami/zookeeper:3.9
    environment:
      ALLOW_ANONYMOUS_LOGIN: "yes"
  kafka:
    image: bitnami/kafka:3.7
    environment:
      KAFKA_CFG_ZOOKEEPER_CONNECT: zookeeper:2181
      ALLOW_PLAINTEXT_LISTENER: "yes"
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  redis:
    image: redis:7
    ports: ["6379:6379"]

  gateway:
    build: ./gateway
    environment:
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      SPRING_PROFILES_ACTIVE: docker
    ports: ["8080:8080"]
    depends_on: [users, workouts, nutrition, achievements, redis]

  users:
    build: ./services/users
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/fittrackr
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on: [postgres]

  workouts:
    build: ./services/workouts
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/fittrackr
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on: [mongo, kafka]

  nutrition:
    build: ./services/nutrition
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/fittrackr
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on: [postgres]

  achievements:
    build: ./services/achievements
    environment:
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/fittrackr
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on: [mongo, kafka]
```

---

## 7. GitHub Actions (CI/CD)

**Monorepo** met matrix build per service. Voorbeeld workflow `/.github/workflows/build.yml`:

```yaml
name: CI
on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build-test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [gateway, services/users, services/workouts, services/nutrition, services/achievements]
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - name: Build & Test
        working-directory: ${{ matrix.service }}
        run: |
          ./mvnw -B -ntp test
          ./mvnw -B -ntp -DskipTests package
      - name: Build Docker image
        run: docker build -t ghcr.io/${{ github.repository }}/${{ matrix.service }}:${{ github.sha }} ${{ matrix.service }}
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Push image
        run: docker push ghcr.io/${{ github.repository }}/${{ matrix.service }}:${{ github.sha }}
```

> Toon in je demo **Actions-historiek** en badges in README.

---

## 8. Testing strategie

* **Unit**: service-layer pure logica (bijv. badge-calculatie) met Mockito.
* **Slice tests**: `@DataJpaTest` / `@DataMongoTest`.
* **Testcontainers** voor Postgres/Mongo/Kafka in CI.
* **Coverage**: mik op ≥80% service package.

Voorbeeld: **AchievementsServiceTest** (pseudocode)

```java
@Test
void grantsFirstWorkoutBadgeOnFirstLogEvent() {
  // given: no badges yet
  // when: consume workouts.logged.v1
  // then: badge FIRST_WORKOUT toegevoegd
}
```

---

## 9. Monitoring (Prometheus/Grafana)

**Services**: voeg toe in `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

**prometheus.yml** (snippet)

```yaml
scrape_configs:
  - job_name: 'gateway'
    static_configs:
      - targets: ['gateway:8080']
  - job_name: 'users'
    static_configs:
      - targets: ['users:8080']
  - job_name: 'workouts'
    static_configs:
      - targets: ['workouts:8080']
  - job_name: 'nutrition'
    static_configs:
      - targets: ['nutrition:8080']
  - job_name: 'achievements'
    static_configs:
      - targets: ['achievements:8080']
```

Maak in Grafana dashboards voor **latency**, **RPS**, **HTTP status codes**.

---

## 10. Kubernetes (optioneel voor extra punten)

Map `k8s/` met bv. `gateway-deploy.yml`, `users-deploy.yml`, ...
**Service types**:

* **ClusterIP**: users/workouts/nutrition/achievements, kafka/mongo/postgres/prometheus
* **NodePort**: **gateway** (expose buiten cluster)

`gateway-service.yml` (voorbeeld)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: gateway
spec:
  type: NodePort
  selector:
    app: gateway
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080
```

---

## 11. Demo-film script (wat zeker tonen)

1. **Thema & use-cases** (2 min): waarom fitness, welke userflows.
2. **Architectuur** (2–3 min): schema + keuzes DB’s & broker.
3. **Belangrijkste endpoints** (4–5 min): Postman calls via Gateway (incl. login).

   * `GET /api/users/me` (na OAuth), `POST /api/workouts/logs`, `GET /api/achievements/me`, `PUT /api/nutrition/day`.
4. **Code walkthrough** (3–4 min): service-layer logica (bv. badge rules), DTO/mapper.
5. **Tests live run** (1–2 min): `mvn test` met groene output.
6. **GitHub Actions** (1 min): geschiedenis, groene pipelines, image pushes.
7. **(Optioneel)** Monitoring dashboard kort tonen.
8. **Afsluiter** (1 min): wat geleerd: microservices, OAuth2, event-driven, CI/CD, observability.

---

## 12. README checklist

* [ ] Thema + korte pitch
* [ ] Architectuurdiagram + component-lijst
* [ ] Endpoints-overzicht (tabel)
* [ ] Postman screenshots (alle CRUD-verplichtingen)
* [ ] Deployment (Compose) + K8s (optioneel)
* [ ] Security (GCP OAuth2 configuratie-stappen)
* [ ] Testing uitleg + coverage screenshot
* [ ] Actions badges + link naar runs
* [ ] Monitoring screenshots (optioneel)
* [ ] Eigen uitbreidingen opsomming

---

## 13. Scoremapping (hoe haal je de punten)

**Basis 60%**

* CRUD via Gateway (≥3 GET, 1 POST, 1 PUT, 1 DELETE)
* Minstens 1×Mongo en 1×SQL
* Documentatie + Postman bewijs
* Docker + Compose + GH Actions
* OAuth2 op Gateway
* Unit tests service-klassen

**Extra**

* 2.1 Front-end: **+15%**
* 2.2 K8s manifests: **+5%**
* 2.2.1 Prometheus: **+20%**
* 2.2.1.1 Grafana: **+15%**
* 2.2.2 ClusterIP/NodePort: **+5%**
* 2.3 Eigen Auth-service: **+25%**
* 2.4 Rate limiting: **+5%**
* 2.5 Event-driven + ActiveMQ/Kafka: **+20%** (**Kafka +15% extra**)

> Tip: kies **Kafka** meteen; dat pakt 2.5 en 2.5.1 samen (twee pods in K8s).

---

## 14. Directory-structuur (monorepo)

```
fittrackr/
  gateway/
    Dockerfile
    src/main/java/... (Spring Cloud Gateway)
  services/
    users/
      Dockerfile
      src/main/java/... (JPA, REST)
    workouts/
      Dockerfile
      src/main/java/... (Mongo, REST, Kafka producer)
    nutrition/
      Dockerfile
      src/main/java/... (JPA, REST)
    achievements/
      Dockerfile
      src/main/java/... (Mongo, REST, Kafka consumer)
  infra/
    docker-compose.yml
    prometheus/
      prometheus.yml
    k8s/ (optioneel)
  frontend/ (optioneel)
  .github/workflows/build.yml
  README.md
```

---

## 15. Snelle start (developer UX)

1. `docker compose up -d postgres mongo zookeeper kafka redis`
2. `./mvnw -q -ntp -DskipTests -pl gateway,services/* package`
3. `docker compose up --build` (services + gateway)
4. Stel **Google OAuth Client** in (Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`).
5. Open Postman: **login via browser**, test endpoints via **Gateway**.

---

## 16. Variaties (als je solo werkt)

* Laat **Nutrition** vallen. Endpoints blijven ruimschoots voldoen.
* Events blijven geldig: Workouts → Achievements.
* Documenteer de aanpassing in README.

---

## 17. Wat learned / reflectie (voor je video)

* Trade-offs SQL vs. NoSQL op datavorm en querypatronen.
* Security op gateway vs. services, token doorsturen (TokenRelay).
* Async events voor losse koppeling en schaalbaarheid.
* CI/CD discipline en traceerbaarheid van builds.
* Observability als feedbacklus (metrics → optimalisaties).

---

### Bijlagen

* **OpenAPI**: genereer met Springdoc (`/v3/api-docs`, `/swagger-ui`) per service.
* **Postman**: maak een collectie ‘FitTrackr – Gateway’ met folders per service en voeg **screenshots** toe in README.

> Klaar! Dit is je blauwdruk. Kopieer dit naar je repo en bouw stap voor stap. Succes met het project en de voorstelling! 💪
