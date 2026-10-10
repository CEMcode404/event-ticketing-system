# Lineup - Event Ticketing System

## Prerequisites

- Java 25
- Node.js 22+ and pnpm
- Docker Desktop
- [Stripe CLI](https://docs.stripe.com/stripe-cli) (`brew install stripe/stripe-cli/stripe`)
- An [Auth0](https://auth0.com) tenant with a SPA application and an API
- A [Stripe](https://stripe.com) account in test mode (sandbox)

## Setup (first time only)

### 1. Backend config

```bash
cp api/src/main/resources/application-local.properties.example api/src/main/resources/application-local.properties
```

Fill in your own values in `application-local.properties`:

- **Auth0:** issuer URI and API audience
- **Stripe:** test secret key (`sk_test_...`) from Dashboard > Developers > API keys
- **Stripe webhook secret:** printed by `stripe listen` in step 2 of "Running the project" (`whsec_...`)

This file is git-ignored, so your keys stay local.

### 2. Frontend config

```bash
cp frontend/.env.local.example frontend/.env.local
```

Fill in your Auth0 domain, client ID, and audience.

### 3. Stripe CLI

```bash
stripe login
```

## Running the project

Use four terminal tabs.

**1. Infrastructure** (MySQL, Redis, DynamoDB Local)

```bash
docker compose up -d
```

Wait until `docker compose ps` shows mysql as `healthy`.

**2. Stripe webhooks** (keep running)

```bash
stripe listen --events checkout.session.completed,checkout.session.expired --forward-to localhost:8080/api/webhooks/stripe
```

**3. API**

```bash
cd api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**4. Frontend**

```bash
cd frontend
pnpm install
pnpm dev
```

Open http://localhost:5173.

## Trying it out

1. Sign in at `/admin/login` and create an event (sale time a few minutes from now).
2. Add a section, then publish the event.
3. Open the home page in a private window and join the event's queue.
4. When admitted, hold seats and check out.
5. Pay with Stripe's test card: `4242 4242 4242 4242`, any future expiry, any CVC.

## Local ports

| Service | Port |
|---|---|
| Frontend | 5173 |
| API | 8080 |
| MySQL | 3306 |
| Redis | 6379 |
| DynamoDB Local | 8000 |

## Useful commands

```bash
docker compose down -v                # reset all local data
pnpm lint && pnpm build               # check the frontend (from frontend/)
```