# Krishi Platform — Architecture Decision Records (ADRs)

---

## ADR 001: Method-Level Security and Explicit IDOR Authorization

- **Status:** Accepted & Enforced
- **Context:** Initial architecture relied primarily on URL-based filter security, which left internal service methods vulnerable to Insecure Direct Object References (IDOR) if endpoints omitted manual user ID verification.
- **Decision:** Enable `@EnableMethodSecurity` globally in Spring Security. Require dual enforcement:
  1. Role validation at endpoint boundaries (`@PreAuthorize("hasRole('FARMER')")`).
  2. Explicit farmer ownership verification within domain services comparing entity ownership to `SecurityContextHolder` authenticated principal.
- **Consequences:** Eliminates IDOR cross-tenant access. Prevents unauthorized farmers from viewing, modifying, or rescheduling tasks, farms, crops, or treatments owned by other users.

---

## ADR 002: Unified Crop Workspace Aggregation Endpoint

- **Status:** Accepted & Enforced
- **Context:** The frontend Crop Details workflow previously made multiple sequential HTTP requests (crop planting, crop cycle, farm, tasks, disease incidents, treatments) creating an API waterfall, slow paint times, and race conditions.
- **Decision:** Implement `GET /api/v1/crop-plantings/{plantingId}/workspace` returning an aggregated payload containing the complete operational workspace (crop, season, farm, tasks, incidents, treatments, active cultivation plan).
- **Consequences:** Reduces network round trips from 6+ sequential calls to 1 atomic request. Simplifies frontend state management and provides cohesive offline caching boundaries.

---

## ADR 003: Versioned Crop Knowledge Catalog and ICAR Agronomic Guidance

- **Status:** Accepted & Enforced
- **Context:** The platform requires cultivation schedules for farmers, but hardcoding schedules in frontend components violates domain integrity, makes regional adaptation impossible, and creates maintenance bottlenecks.
- **Decision:** Establish a relational, versioned knowledge catalog in PostgreSQL (`cultivation_templates`, `cultivation_stages`, `cultivation_template_tasks`) seeded exclusively from validated Indian Council of Agricultural Research (ICAR) packages of practice (Rice/Paddy, Tomato, Wheat).
- **Consequences:** UI components dynamically render catalog guidance without hardcoded durations or invented chemical/fertilizer dosages. Extension officers can version and publish updated packages without requiring frontend deployments.

---

## ADR 004: Cultivation Plan Generation, Idempotency, and Task History Preservation

- **Status:** Accepted & Enforced
- **Context:** When farmers create plantings or revise cultivation parameters, generating tasks must not create duplicates upon network retries, and revising plans must not destroy existing work records.
- **Decision:**
  1. Make `generatePlanForPlanting()` strictly idempotent: retrying with the same template ID returns the active plan without duplicate task generation.
  2. When a farmer revises an existing plan with a new template, archive the old plan as `SUPERSEDED`, preserve historical tasks (`COMPLETED`, `IN_PROGRESS`, `SKIPPED`), and supersede only unstarted `PENDING` plan tasks.
- **Consequences:** Guarantees historical auditability of field work while allowing dynamic plan adjustments.

---

## ADR 005: Dual-Branch Development Model (Core Platform vs. Python AI Service)

- **Status:** Accepted & Enforced
- **Context:** Two developers are working concurrently: Developer A on core operational platform (Java / Spring Boot / React) and Developer B on AI computer vision and advisory models (Python / PyTorch). Shared commits on a single branch risk unstable baselines and merge conflicts.
- **Decision:**
  - `main`: Clean integration branch containing only verified, regression-tested production baselines.
  - `feature/core-platform`: Working branch for Developer A (farm operations, tasks, knowledge catalog, frontend).
  - `feature/ai-service`: Working branch for Developer B (independent Python microservice, ML models, AI doctor).
- **Consequences:** Complete isolation of dependencies (Java 17 vs Python 3.11). Developers coordinate via documented REST contracts (`docs/integration/AI_INTEGRATION_CONTRACT.md`) without blocking each other's local repositories.

---

## ADR 006: Dedicated Test Database Guard and Flyway Validation Invariants

- **Status:** Accepted & Enforced
- **Context:** Integration tests run against PostgreSQL and execute Flyway schema migrations. Connecting to a shared or production database could cause catastrophic data corruption.
- **Decision:** `TestDatabaseInitializer` strictly validates that the test datasource targets `jdbc:postgresql://localhost:5432/krishi_test_db`. If the URL targets any other database, tests immediately abort before executing migrations or queries.
- **Consequences:** Eliminates risks of test fixtures contaminating production or development databases.

---

## ADR 007: Database-Level Enforced Single Active Cultivation Plan Invariant & JPA Flush Ordering

- **Status:** Accepted & Enforced
- **Context:** If concurrent HTTP requests attempt to generate cultivation plans for the same crop planting, application-level checks alone cannot prevent race conditions. Duplicate `ACTIVE` plans would result in orphaned tasks and non-deterministic task scheduling. Additionally, under Hibernate/JPA, replacing an entity constrained by a partial unique index can fail if Hibernate queues the `INSERT` before the `UPDATE` of the old entity.
- **Decision:**
  1. Enforce a PostgreSQL partial unique index via forward migration `V19__enforce_single_active_cultivation_plan.sql`:
     `CREATE UNIQUE INDEX IF NOT EXISTS uq_active_cultivation_plan_per_planting ON krishi.cultivation_plans (crop_planting_id) WHERE status = 'ACTIVE';`
  2. In `CultivationPlanService.java`, execute `planRepository.saveAndFlush(existingPlan)` prior to constructing and saving the new plan, ensuring the SQL `UPDATE` to `SUPERSEDED` is executed immediately on the database connection before the `INSERT` of the new `ACTIVE` plan.
- **Consequences:** Provides hard database guarantees against duplicate active cultivation plans. Prevents constraint violation exceptions during plan revisions.

---

## ADR 008: Weather Intelligence Provider Abstraction & Open-Meteo Integration

- **Status:** Accepted & Implemented (Milestone 3B)
- **Context:** Krishi requires accurate meteorological forecasts and agricultural parameters (precipitation, temperature, humidity, wind, and soil conditions) mapped to Indian farm coordinates to power operational advisories and field activity planning. Hardcoding a single commercial vendor or relying on APIs that require high fees or complex authentication creates operational friction and vendor lock-in.
- **Decision:**
  1. Define a domain interface `WeatherDataProvider` that abstracts weather retrieval, returning canonical domain objects (`WeatherObservation`, `DailyForecast`, `HourlyForecast`).
  2. Implement an `OpenMeteoProviderClient` leveraging Open-Meteo's API with ECMWF IFS / GFS numerical models. Open-Meteo provides global coverage, high resolution over India, agricultural variables, 10,000 free requests/day for non-commercial use, and full CC BY 4.0 attribution compliance with UI hyperlink. Support optional API keys for commercial tiers.
  3. Implement a resilient spatial caching layer (`krishi.weather_forecast_cache`) normalized to 2 decimal places (~1.1 km precision) with a 2-hour TTL and 15-minute manual refresh cooldown. If the external provider is unreachable or times out, serve valid cached data with a `stale: true` indicator. Never fabricate synthetic meteorological values.
  4. Use farm GPS coordinates when available; fall back to verified district centroid coordinates seeded from Census of India 2011 / NIC portals. If neither is available, return an explicit HTTP 422 `LOCATION_UNAVAILABLE` error prompt.
- **Consequences:** Decouples core platform from external weather APIs. Ensures low-latency, resilient responses while keeping operating costs zero during early adoption.

---

## ADR 009: Contextual Agricultural Advisories and Non-Intrusive Task Guidance

- **Status:** Accepted & Implemented (Milestone 3B)
- **Context:** Adverse meteorological events (heavy downpours, strong winds, extreme heat) impact pending field work (irrigation, spraying, harvesting). However, automatically rescheduling, cancelling, or completing tasks without farmer knowledge violates agronomic sovereignty and could result in disastrous operational decisions if conditions vary locally.
- **Decision:**
  1. Evaluate weather forecasts against a defensible, ICAR-aligned rules engine producing `WeatherAdvisory` records. Start in V1 with 3 strictly verified operational warning categories:
     - `HEAVY_RAIN_IRRIGATION`: Rain >= 20mm or prob >= 80% on pending irrigation tasks.
     - `HIGH_WIND_SPRAYING`: Sustained wind > 20 km/h or gusts > 30 km/h on pending spraying/fertilizing tasks.
     - `EXTREME_HEAT_ALERT`: Max temperature >= 40.0°C for active plantings.
  2. Defer unverified agronomic predictions (fungal blast microclimate models, frost warnings) pending cultivar-level sensor calibration.
  3. When an advisory impacts a scheduled task, link the advisory to the task (`task_id`) and render contextual warning chips on task cards and modal guidance. Never mutate task state, due dates, or completion data automatically.
  4. Enforce idempotency and deduplication via unique composite fingerprints (`farmId:plantingId:taskId:ruleCode:targetDate`) backed by a partial unique index (`WHERE status = 'ACTIVE'`).
  5. If conditions worsen, supersede the previous advisory, insert the escalated alert, and dispatch a notification. If conditions remain stable, update `lastEvaluatedAt` without re-notifying the farmer.
- **Consequences:** Empowers farmers with actionable contextual intelligence while preserving operational control and preventing alert fatigue.

---

## ADR 010: Concurrency-Safe Scheduler Advisory Locking & Immediate Action Queue Flushing

- **Status:** Accepted & Implemented (Milestone 3B)
- **Context:** Scheduled background jobs (e.g. daily weather forecast sync across registered farms) in a multi-instance or clustered environment risk race conditions, redundant API consumption, and rate-limit exhaustion if multiple workers execute simultaneously. Furthermore, Hibernate's action queue delays entity UPDATE operations relative to INSERTs within a transaction, which triggers unique constraint violations on partial indexes (`WHERE status = 'ACTIVE'`) during entity supersession.
- **Decision:**
  1. Guard the scheduled weather synchronization job with a 64-bit PostgreSQL session-level advisory lock (`SELECT pg_try_advisory_lock(?)`) pinned to a single physical JDBC `Connection` via `DataSource.getConnection()`.
  2. Wrap lock acquisition and execution in a `try-with-resources` construct with an explicit `pg_advisory_unlock` in the `finally` block on the exact same connection instance. If the connection drops or is closed, PostgreSQL guarantees automatic lock release, completely eliminating orphaned locks and overlapping executions.
  3. When superseding active entities (e.g. transitioning an advisory from `ACTIVE` to `SUPERSEDED` upon severity escalation), call `repository.saveAndFlush(existing)` prior to creating and saving the new `ACTIVE` entity. This forces the SQL `UPDATE` to execute on the database connection immediately, preventing unique constraint collisions against the partial index.
- **Consequences:** Guarantees zero overlapping scheduled jobs without requiring distributed lock brokers like Redis or ZooKeeper, and prevents unique constraint violations during entity escalation.
