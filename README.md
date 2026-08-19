# Document parsing and processing app

A Micronaut-based application which contains a html test client and a service that accepts document uploads, parses them via an external lambda parser service, persists results to MongoDB, and notifies clients over WebSocket. It includes structured JSON logging with correlation ID, retry/fallback handling for the parser. It also contains an independent RabbitMQ consumer which can be triggered only by RabbitMQ management UI.

---

## How to Run

### Prerequisites
- JDK 21+ (or whatever your `build.gradle`/`pom.xml` targets)
- Docker + Docker Compose (for MongoDB and RabbitMQ locally)
- Gradle / Maven wrapper

### 1. Start dependencies

```bash
docker compose up -d mongo rabbitmq
```

This brings up:
- **MongoDB** — document persistence
- **RabbitMQ** — used only by the standalone consumer (Start this if you want to try and trigger the consumer via RabbitMQ Management UI)

### 2. Configure environment

Copy the sample config and adjust as needed:

```bash
cp application-sample.properties application.properties
```

### 3. Run the application

```bash
./gradlew run
```
or
```bash
./mvnw mn:run
```

### 4. Upload a document

Via the test client / UI, select a file and submit. Progress and results are pushed to the client over WebSocket

### 5. Logs

Logs are emitted as structured JSON on the console that ties every log line for a given request together useful for tracing a single upload through parsing, persistence, and notification.

---

## Design Decisions

### Reactive, non-blocking pipeline
The upload path (`DocumentProcessingService.processUpload`) is built on Project Reactor (`Mono`/`Flux`). File bytes are read synchronously up front, then content-type detection, parsing, and persistence are chained reactively.

### Declarative HTTP client with retry + fallback
The external parser is called via a Micronaut declarative `@Client` interface. It support 3 retries with a total 4 attempts with expotential backoff capping at 1s between attempts. Once exhausted a fallback will be triggered.

### Parser "soft failure" is treated as a hard failure
The parser can return a `200 OK` with a body indicating logical failure. For this instance, this is explicitly checked and converted into a reactive error, rather than being silently treated as a successful result. But depending the requirement for fallback, its behavior can be changed.

### MongoDB is a hard dependency; RabbitMQ is not
The entire upload workflow depends on whether MongoDB is up and running. Parsing still work but the entire call chain from persistence up to broadcasting the result to client will stop working.

RabbitMQ will not affect the flow of the application as it has its own independent consumer outside the scope or workflow.

### WebSocket notifications are independent of the broker
Client notifications are pushed directly from the reactive pipeline and do not go through RabbitMQ, so they continue to function even if the broker is down.

---

## Known Limitations
- **No retry on the MongoDB save step.** 
Retry/fallback is only implemented for the external parser call
- **Content-type detection and empty-file checks return silently rather than a typed error**, meaning some rejected-upload cases are distinguishable only via the WebSocket failure notification, not via the returned `Mono`'s signal type. Callers relying purely on reactive error handling need to be aware these paths complete rather than error.
