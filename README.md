# Lineup — Event Ticketing System

## Prerequisites

- Java 25
- Node.js 22+ and pnpm 10+
- Docker Desktop
- [DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html) (JAR version)
- An Auth0 tenant with a SPA application and an API configured

## Setup

### 1. Start local infrastructure

```bash
./dev-up.sh
```

Starts MySQL and Redis via Docker Compose, plus DynamoDB Local natively on port `8000`.

### 2. Configure environment

**Backend:** set in `api/src/main/resources/application.yml` or as environment variables:

```
AUTH0_ISSUER_URI=https://<your-tenant>.auth0.com/
AUTH0_AUDIENCE=<your-api-identifier>
```

**Frontend:** create `frontend/.env.local`:

```
VITE_AUTH0_DOMAIN=<your-tenant>.auth0.com
VITE_AUTH0_CLIENT_ID=<your-spa-client-id>
VITE_AUTH0_AUDIENCE=<your-api-identifier>
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Run the backend

```bash
cd api
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`.

### 4. Run the frontend

```bash
cd frontend
pnpm install
pnpm dev
```

Runs on `http://localhost:5173`.

## Local ports

| Service | Port |
|---|---|
| Frontend | 5173 |
| API | 8080 |
| DynamoDB Local | 8000 |
| MySQL | 3306 |
| Redis | 6379 |