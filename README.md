# Alumni LinkedIn Profile Searcher

A production-oriented Spring Boot REST API that searches for LinkedIn alumni profiles from a specified educational institution using the **PhantomBuster** API and persists the results in PostgreSQL.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Quick Start](#quick-start)
3. [Evaluator Setup](#evaluator-setup)
4. [Features](#features)
5. [Architecture](#architecture)
6. [Technology Stack](#technology-stack)
7. [Project Structure](#project-structure)
8. [Prerequisites](#prerequisites)
9. [Environment Variables](#environment-variables)
10. [PostgreSQL Setup (Docker)](#postgresql-setup-docker)
11. [PhantomBuster Setup](#phantombuster-setup)
12. [How to Run](#how-to-run)
13. [API Documentation](#api-documentation)
14. [Postman Instructions](#postman-instructions)
15. [Testing](#testing)
16. [Error Handling](#error-handling)
17. [Design Decisions](#design-decisions)
18. [Limitations and Assumptions](#limitations-and-assumptions)

---

## Project Overview

This application exposes two REST endpoints:

| Method | Endpoint             | Description                                        |
|--------|----------------------|----------------------------------------------------|
| POST   | `/api/alumni/search` | Search LinkedIn alumni via PhantomBuster, persist  |
| GET    | `/api/alumni/all`    | Retrieve all saved alumni profiles                 |

The search endpoint accepts a university name, a current designation, and an optional graduation year. It delegates to PhantomBuster's **LinkedIn Search Export** Phantom, waits for the asynchronous run to complete, maps the results to the domain model, deduplicates against the existing database, and returns the matched profiles.

---

## Quick Start

### 1. Clone the repository

```bash
git clone <repository-url>
cd alumni-linkedin-profile-searcher
```

### 2. Configure environment variables

```bash
cp .env.example .env   # Linux/macOS
copy .env.example .env  # Windows
```

Open `.env` and fill in your values — at minimum:

```
DB_URL=jdbc:postgresql://localhost:5432/alumnidb
DB_USERNAME=alumniuser
DB_PASSWORD=alumnipass
POSTGRES_DB=alumnidb
POSTGRES_USER=alumniuser
POSTGRES_PASSWORD=alumnipass

PHANTOMBUSTER_API_KEY=<your_phantombuster_api_key>
PHANTOMBUSTER_AGENT_ID=<your_linkedin_search_export_agent_id>
```

### 3. Start PostgreSQL

```bash
docker compose up -d
```

### 4. Load environment variables into your shell

> **Important:** Spring Boot does not automatically read `.env` files.
> You must export the variables into your shell before starting the application.
> Use the commands below — **PowerShell only on Windows, not CMD**.

**Windows (PowerShell)**

```powershell
Get-Content .env | Where-Object { $_ -notmatch '^#' -and $_ -ne '' } | ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
}
```

**Linux / macOS**

```bash
export $(grep -v '^#' .env | xargs)
```

### 5. Run the application

**Windows**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## Evaluator Setup

This repository contains **no personal PhantomBuster or LinkedIn credentials**.

To test the application, provide your own:

- `PHANTOMBUSTER_API_KEY` — from your PhantomBuster workspace settings
- `PHANTOMBUSTER_AGENT_ID` — the Agent ID of a LinkedIn Search Export Phantom in your workspace
- A LinkedIn account connected to the Phantom via the PhantomBuster browser extension

The application does not store or manage LinkedIn credentials directly. All LinkedIn authentication is handled by the pre-configured Phantom inside your PhantomBuster account.

See [PhantomBuster Setup](#phantombuster-setup) for step-by-step instructions.

---

## Features

- REST API with consistent JSON response envelope (`status` + `data`)
- Full Jakarta Bean Validation on all request inputs
- PhantomBuster v2 API integration: launch → async poll → parse result
- PostgreSQL persistence via Spring Data JPA and Flyway migrations
- Deduplication by LinkedIn profile URL — prevents duplicate records across multiple searches
- Centralised exception handling with correct HTTP status codes
- Comprehensive unit tests (service, controller, exception handler)
- Docker Compose for zero-config local PostgreSQL
- Postman collection for manual testing

---

## Architecture

```
HTTP Request
     │
     ▼
AlumniController          ← validates input, delegates, returns response
     │
     ▼
AlumniServiceImpl         ← orchestrates workflow, deduplication, transactions
     │              │
     ▼              ▼
PhantomBusterClient   AlumniRepository
  (interface)         (Spring Data JPA)
     │
     ▼
PhantomBusterClientImpl
  POST /api/v2/agents/launch       → containerId
  GET  /api/v2/containers/fetch-result-object  (poll until finished)
  parse resultObject JSON array
     │
     ▼
AlumniMapper              ← converts external DTOs ↔ entities ↔ response DTOs
```

---

## Technology Stack

| Layer        | Technology                         |
|--------------|------------------------------------|
| Language     | Java 21                            |
| Framework    | Spring Boot 3.3.4                  |
| HTTP client  | Spring RestClient (Spring 6.1+)    |
| Persistence  | Spring Data JPA + Hibernate 6      |
| Database     | PostgreSQL 16                      |
| Migrations   | Flyway                             |
| Validation   | Jakarta Bean Validation            |
| Boilerplate  | Lombok                             |
| Build        | Maven 3                            |
| Testing      | JUnit 5, Mockito, MockMvc, H2      |
| Container    | Docker + Docker Compose            |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/alumni/
│   │   ├── AlumniApplication.java
│   │   ├── controller/
│   │   │   └── AlumniController.java
│   │   ├── service/
│   │   │   ├── AlumniService.java
│   │   │   └── AlumniServiceImpl.java
│   │   ├── repository/
│   │   │   └── AlumniRepository.java
│   │   ├── entity/
│   │   │   └── Alumni.java
│   │   ├── dto/
│   │   │   ├── request/AlumniSearchRequest.java
│   │   │   ├── response/AlumniResponse.java
│   │   │   ├── response/ApiResponse.java
│   │   │   └── external/
│   │   │       ├── LinkedInProfileResult.java
│   │   │       ├── PhantomBusterLaunchRequest.java
│   │   │       ├── PhantomBusterLaunchResponse.java
│   │   │       └── PhantomBusterResultResponse.java
│   │   ├── client/
│   │   │   ├── PhantomBusterClient.java
│   │   │   └── PhantomBusterClientImpl.java
│   │   ├── mapper/
│   │   │   └── AlumniMapper.java
│   │   ├── config/
│   │   │   ├── PhantomBusterProperties.java
│   │   │   └── RestClientConfig.java
│   │   └── exception/
│   │       ├── GlobalExceptionHandler.java
│   │       ├── PhantomBusterException.java
│   │       ├── AlumniDataException.java
│   │       └── ErrorResponse.java
│   └── resources/
│       ├── application.properties
│       ├── application-test.properties
│       └── db/migration/
│           └── V1__create_alumni_table.sql
└── test/
    └── java/com/example/alumni/
        ├── controller/AlumniControllerTest.java
        ├── service/AlumniServiceImplTest.java
        └── exception/GlobalExceptionHandlerTest.java

postman/
└── Alumni-Profile-Searcher.postman_collection.json
docker-compose.yml
.env.example
run.ps1          ← convenience script for Windows
```

---

## Prerequisites

- **Java 21** — [Download Temurin](https://adoptium.net/)
- **Maven 3.9+** — or use the included Maven wrapper (`mvnw` / `mvnw.cmd`)
- **Docker Desktop** — for running PostgreSQL via Docker Compose
- **PhantomBuster account** — free trial is sufficient for testing

---

## Environment Variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

| Variable                          | Required | Description                                               |
|-----------------------------------|----------|-----------------------------------------------------------|
| `DB_URL`                          | ✅       | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/alumnidb`|
| `DB_USERNAME`                     | ✅       | PostgreSQL username                                       |
| `DB_PASSWORD`                     | ✅       | PostgreSQL password                                       |
| `POSTGRES_DB`                     | ✅       | Database name (must match `DB_URL`)                       |
| `POSTGRES_USER`                   | ✅       | DB user (used by docker-compose)                          |
| `POSTGRES_PASSWORD`               | ✅       | DB password (used by docker-compose)                      |
| `PHANTOMBUSTER_API_KEY`           | ✅       | Your PhantomBuster workspace API key                      |
| `PHANTOMBUSTER_BASE_URL`          | ❌       | Defaults to `https://api.phantombuster.com/api/v2`        |
| `PHANTOMBUSTER_AGENT_ID`          | ✅       | Agent ID of your LinkedIn Search Export Phantom           |
| `PHANTOMBUSTER_POLL_INTERVAL_MS`  | ❌       | Milliseconds between polls. Default: `3000`               |
| `PHANTOMBUSTER_POLL_MAX_ATTEMPTS` | ❌       | Max polls before timeout. Default: `20` (~60 s total)     |

**Never commit `.env` to Git.** Only `.env.example` (with placeholder values) is committed.

---

## PostgreSQL Setup (Docker)

Start PostgreSQL:

```bash
docker compose up -d
```

Verify it is healthy:

```bash
docker compose ps
```

Stop (data is preserved in the Docker volume):

```bash
docker compose down
```

Full reset including all data:

```bash
docker compose down -v
```

Flyway automatically applies `V1__create_alumni_table.sql` on the first application startup.

---

## PhantomBuster Setup

PhantomBuster is a web automation platform. The application uses its **LinkedIn Search Export** Phantom to search LinkedIn and return profile data.

### Step 1 — Create a PhantomBuster account

Sign up at [phantombuster.com](https://phantombuster.com). The free trial is sufficient.

### Step 2 — Create the LinkedIn Search Export Phantom

1. Click **Solutions** in the top navigation.
2. Search for **LinkedIn Search Export** and click **Use this Phantom**.
3. Connect your LinkedIn account using the PhantomBuster browser extension.
4. Save the Phantom.

   > The application supplies the LinkedIn Search URL dynamically when launching
   > the Phantom through the API. You do not need to configure search URLs manually.

5. Launch it once manually from the dashboard to confirm authentication works before using the API.

### Step 3 — Get your API key

1. Go to **Workspace Settings** → **API keys**.
2. Click **Add API key**, copy it immediately (shown only once).
3. Set `PHANTOMBUSTER_API_KEY=<your_key>` in `.env`.

### Step 4 — Get the Agent ID

1. Open your LinkedIn Search Export Phantom in the PhantomBuster dashboard.
2. Copy the Agent ID from the dashboard.

   The most reliable way is to look at the URL when the Phantom console is open:
   ```
   https://phantombuster.com/<org_id>/phantoms/<template_id>/<agent_id>/console
   ```
   The **last numeric segment before `/console`** is your Agent ID.

   Alternatively, verify it using the API:
   ```bash
   curl -H "X-Phantombuster-Key-1: <your_api_key>" \
        https://api.phantombuster.com/api/v2/agents/fetch-all
   ```
   Find the entry named "LinkedIn Search Export" (or whatever you named it) and copy its `id` field.

3. Set `PHANTOMBUSTER_AGENT_ID=<agent_id>` in `.env`.

---

## How to Run

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Load environment variables

> **Spring Boot does not read `.env` files automatically.**
> You must export the variables before starting the application.
> On Windows, use **PowerShell** — not CMD.

**Windows (PowerShell)**

```powershell
Get-Content .env | Where-Object { $_ -notmatch '^#' -and $_ -ne '' } | ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
}
```

Or use the included convenience script:

```powershell
.\run.ps1
```

**Linux / macOS**

```bash
export $(grep -v '^#' .env | xargs)
```

### 3. Run the application

**Windows**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

### 4. Build a JAR (optional)

**Windows**

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target/alumni-linkedin-profile-searcher-1.0.0.jar
```

**Linux / macOS**

```bash
./mvnw clean package -DskipTests
java -jar target/alumni-linkedin-profile-searcher-1.0.0.jar
```

The application starts on `http://localhost:8080`.

---

## API Documentation

### POST `/api/alumni/search`

Searches LinkedIn for alumni profiles matching the given university and designation, persists new profiles, and returns the matched set.

**Request Body**

```json
{
  "university":  "IIT Bombay",
  "designation": "Software Engineer",
  "passoutYear": 2020
}
```

| Field       | Type    | Required | Constraints               |
|-------------|---------|----------|---------------------------|
| university  | String  | ✅       | Non-blank                 |
| designation | String  | ✅       | Non-blank                 |
| passoutYear | Integer | ❌       | 1950–2100 if provided     |

**Success Response — 200 OK**

```json
{
  "status": "success",
  "data": [
    {
      "name": "John Doe",
      "currentRole": "Software Engineer at XYZ Corp",
      "university": "IIT Bombay",
      "location": "Bangalore, India",
      "linkedinHeadline": "Software Engineer @ XYZ | IIT Bombay",
      "passoutYear": 2020
    }
  ]
}
```

**Error Responses**

| Status | Cause                               |
|--------|-------------------------------------|
| 400    | Missing or invalid request fields   |
| 405    | Wrong HTTP method                   |
| 502    | PhantomBuster API failure           |
| 500    | Unexpected server or database error |

---

### GET `/api/alumni/all`

Returns all alumni profiles saved in the database.

**Success Response — 200 OK**

```json
{
  "status": "success",
  "data": [
    {
      "name": "John Doe",
      "currentRole": "Software Engineer at XYZ Corp",
      "university": "IIT Bombay",
      "location": "Bangalore, India",
      "linkedinHeadline": "Software Engineer @ XYZ | IIT Bombay",
      "passoutYear": 2020
    }
  ]
}
```

Returns an empty `data` array if no alumni have been saved yet.

---

### Error Response Shape

All non-2xx responses use this consistent structure:

```json
{
  "status":    "error",
  "message":   "Meaningful description of what went wrong",
  "timestamp": "2026-09-04T15:30:00"
}
```

Stack traces and internal exception details are never included.

---

## Postman Instructions

1. Open Postman and click **Import**.
2. Select `postman/Alumni-Profile-Searcher.postman_collection.json`.
3. The collection variable `baseUrl` is pre-set to `http://localhost:8080`. Change it under the collection's **Variables** tab if your port differs.
4. Run requests in this order:

**Search Alumni** — POST `/api/alumni/search`

```json
{
  "university":  "IIT Bombay",
  "designation": "Software Engineer",
  "passoutYear": 2020
}
```

Expected result with PhantomBuster configured: **HTTP 200** with matched profiles.  
Expected result without PhantomBuster configured: **HTTP 502** with an error message.

**Get All Alumni** — GET `/api/alumni/all`

Expected: **HTTP 200** with all previously saved profiles (empty array if none saved yet).

**Validation examples** (no PhantomBuster needed):
- Missing `university` → **400**
- Missing `designation` → **400**
- `passoutYear: 1900` → **400**
- GET on `/api/alumni/search` → **405**

---

## Testing

Run all tests:

**Windows**

```powershell
.\mvnw.cmd test
```

**Linux / macOS**

```bash
./mvnw test
```

Tests use an **H2 in-memory database** activated via the `test` Spring profile. No PostgreSQL or PhantomBuster connection is required to run the test suite.

### What is tested

| Test class                   | Scenarios covered                                                                                     |
|------------------------------|-------------------------------------------------------------------------------------------------------|
| `AlumniServiceImplTest`      | Successful search, multiple profiles, empty result, PhantomBuster exception, deduplication, year passthrough, all-alumni retrieval |
| `AlumniControllerTest`       | Valid search, empty result, missing university, missing designation, blank university, year too low, year too high, optional year omitted, 502 upstream error, get-all happy path, get-all empty |
| `GlobalExceptionHandlerTest` | Validation → 400, PhantomBuster → 502, AlumniData → 500, DataAccess → 500, unexpected → 500, wrong method → 405, no stack trace in response |

Total: **28 tests, 0 failures**.

---

## Error Handling

| Scenario                         | HTTP Status | Notes                                      |
|----------------------------------|-------------|--------------------------------------------|
| Missing / blank required field   | 400         | All validation errors returned in one message |
| Wrong HTTP method                | 405         | e.g. GET on a POST-only endpoint           |
| PhantomBuster connection failure | 502         | Upstream dependency error                  |
| PhantomBuster timeout            | 502         | Configurable via `PHANTOMBUSTER_POLL_MAX_ATTEMPTS` |
| PhantomBuster HTTP 4xx / 5xx     | 502         | Response body included in log              |
| Invalid PhantomBuster response   | 502         | Unparseable result                         |
| Domain data error                | 500         | AlumniDataException                        |
| Database / JPA error             | 500         | DataAccessException                        |
| Unhandled exception              | 500         | Catch-all                                  |

---

## Design Decisions

### PhantomBuster asynchronous workflow

PhantomBuster runs Phantoms asynchronously. The application:
1. Calls `POST /api/v2/agents/launch` → receives `containerId`
2. Polls `GET /api/v2/containers/fetch-result-object?id={containerId}` until status is `"finished"` or `"error"`
3. Parses the `resultObject` JSON array into profile DTOs

Polling interval and timeout are configurable via `PHANTOMBUSTER_POLL_INTERVAL_MS` and `PHANTOMBUSTER_POLL_MAX_ATTEMPTS`.

### PhantomBusterClient interface

The service depends on `PhantomBusterClient` (interface), not the HTTP implementation. This decouples the service from HTTP details and makes it testable with a mock. If PhantomBuster is replaced by another data source, only the implementation changes.

### Deduplication by LinkedIn profile URL

PhantomBuster's LinkedIn Search Export returns a `profileUrl` for each result. This is used as a stable, natural deduplication key. Before inserting, the service calls `findByProfileUrl` and skips any profile already in the database. A partial unique index (`WHERE profile_url IS NOT NULL`) enforces this at the database level.

### Flyway for schema management

Flyway manages the schema rather than `ddl-auto=create`. This gives deterministic, version-controlled schema evolution. The test profile uses `ddl-auto=create-drop` with H2 for simplicity.

### Transaction boundary

`@Transactional` wraps only the `persistNewProfiles` batch operation, not the full search method. This ensures no database connection is held open during the PhantomBuster HTTP call, and a persistence failure rolls back only the current batch insert.

### Constructor injection

All beans use constructor injection. `@Autowired` field injection is not used anywhere. Dependencies are `final`, making them explicit and immutable.

---

## Limitations and Assumptions

1. **Pass-out year filtering** — PhantomBuster's LinkedIn Search Export does not return a graduation year in its output. The `passoutYear` parameter is stored against persisted profiles for data-provenance purposes but does not filter the LinkedIn search results. This is a limitation of the external API, not the application design.

2. **LinkedIn session required** — The LinkedIn Search Export Phantom must have a valid LinkedIn session cookie configured in the PhantomBuster dashboard before the application can use it. The application does not manage LinkedIn authentication.

3. **LinkedIn rate limits** — LinkedIn limits search scraping to approximately 1,000 results per day per account. PhantomBuster enforces safe limits internally.

4. **Synchronous from the caller's perspective** — The search endpoint blocks while waiting for PhantomBuster to finish (up to ~60 seconds at default settings). This is acceptable for the assignment scope.

5. **Single Phantom instance** — One pre-configured LinkedIn Search Export Phantom is used. Concurrent requests compete for the same Phantom. For multi-user production use, a Phantom pool would be required.

6. **Profiles without a LinkedIn URL** — In rare cases PhantomBuster may return a profile without a `profileUrl`. Such profiles are inserted without deduplication since no stable key is available to compare against.
